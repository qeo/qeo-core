#import "QGRGraphViewController.h"
#import "QGRIfaceSpeedData.h"
#import "QGRCachedGraphData.h"

static const double RefreshRate = 5.0;
static const double xMaxCount = 50;
@interface QGRGraphViewController ()

-(void)plotGraph:(NSNotification *)notification;
@end

@implementation QGRGraphViewController

{
    NSMutableArray *inbytes;
    NSMutableArray *outbytes;
    NSTimer *timer;
    NSUInteger currentIndex;
    CPTGraph *graph;
}



- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.autoresizesSubviews = YES;
    
    if ([self respondsToSelector:@selector(edgesForExtendedLayout)]) {
        self.edgesForExtendedLayout = UIRectEdgeNone;
    }
    
    inbytes=[[NSMutableArray alloc]init];
    outbytes=[[NSMutableArray alloc]init];
	// Do any additional setup after loading the view.
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(plotGraph:)
                                                 name:@"graphData" object:nil];
    
    
}
#pragma mark - UIViewController lifecycle methods
-(void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self initPlot];
}

#pragma mark - Chart behavior
-(void)initPlot {
    [self configureHost];
    [self configureGraph];
    [self configurePlots];
    [self configureAxes];
    
}


-(void)configureHost {
    self.hostView = [(CPTGraphHostingView *) [CPTGraphHostingView alloc] initWithFrame:self.view.bounds];
 
    [self.view addSubview:self.hostView];
    
}


-(void)configureGraph {
    
    // 1 - Create the graph
    graph = [[CPTXYGraph alloc] initWithFrame:self.hostView.bounds];
    [graph applyTheme:[CPTTheme themeNamed:kCPTDarkGradientTheme]];
    self.hostView.autoresizingMask=(UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    self.hostView.hostedGraph = graph;
    
    // remove default border
    graph.plotAreaFrame.borderLineStyle = nil;
    graph.plotAreaFrame.cornerRadius = 0.0f;
    
    // 2 - Set graph title
    NSString *title = @"Data Graph (In-Red,Out-Green )";
    graph.title = title;
    // 3 - Create and set text style
    CPTMutableTextStyle *titleStyle = [CPTMutableTextStyle textStyle];
    titleStyle.color = [CPTColor whiteColor];
    titleStyle.fontName = @"Helvetica-Bold";
    titleStyle.fontSize = 12.0f;
    graph.titleTextStyle = titleStyle;
    graph.titlePlotAreaFrameAnchor = CPTRectAnchorTop;
    graph.titleDisplacement = CGPointMake(0.0f, 0.0f);
    // 4 - Set padding for plot area
    [graph.plotAreaFrame setPaddingTop:15.0f];
    [graph.plotAreaFrame setPaddingLeft:50.0f];
    [graph.plotAreaFrame setPaddingBottom:40.0f];
    // 5 - Enable user interactions for plot space
    CPTXYPlotSpace *plotSpace = (CPTXYPlotSpace *) graph.defaultPlotSpace;
    plotSpace.allowsUserInteraction = NO;//YES to let user zoom
}

-(void)configurePlots {
    
    // 1 - Get graph and plot space
   CPTXYPlotSpace *plotSpace = (CPTXYPlotSpace *) graph.defaultPlotSpace;
    
    // 2 - Create the in/out plots
    CPTScatterPlot *inBytePlot = [[CPTScatterPlot alloc] init];
    inBytePlot.dataSource = self;
    inBytePlot.identifier = @"inBytes";
    CPTColor *inByteColor = [CPTColor redColor];
    [graph addPlot:inBytePlot toPlotSpace:plotSpace];
    CPTScatterPlot *outBytePlot = [[CPTScatterPlot alloc] init];
    outBytePlot.dataSource = self;
    outBytePlot.identifier = @"outBytes";
    CPTColor *outByteColor = [CPTColor greenColor];
    [graph addPlot:outBytePlot toPlotSpace:plotSpace];
    
    // 3 - Setup initial plot space
    //     - autoscale x and y axis
    //     - reset x-axis to fixed range
    [plotSpace scaleToFitPlots:[graph allPlots]];
    plotSpace.xRange = [CPTPlotRange plotRangeWithLocation:CPTDecimalFromInteger(0) length:CPTDecimalFromInteger(50)];
    
    
    // 4 - Create styles and symbols
    CPTMutableLineStyle *inByteLineStyle = [inBytePlot.dataLineStyle mutableCopy];
    inByteLineStyle.lineWidth = 1.0;
    inByteLineStyle.lineColor = inByteColor;
    inBytePlot.dataLineStyle = inByteLineStyle;
    CPTMutableLineStyle *inBytelSymbolLineStyle = [CPTMutableLineStyle lineStyle];
    inBytelSymbolLineStyle.lineColor = inByteColor;
    CPTPlotSymbol *inByteSymbol = [CPTPlotSymbol trianglePlotSymbol];
    inByteSymbol.fill = [CPTFill fillWithColor:inByteColor];
    inByteSymbol.lineStyle = inBytelSymbolLineStyle;
    inByteSymbol.size = CGSizeMake(6.0f, 6.0f);
    inBytePlot.plotSymbol = inByteSymbol;
    
    //outByte style
    CPTMutableLineStyle *outByteLineStyle = [outBytePlot.dataLineStyle mutableCopy];
    outByteLineStyle.lineWidth = 1.0;
    outByteLineStyle.lineColor = outByteColor;
    outBytePlot.dataLineStyle = outByteLineStyle;
    CPTMutableLineStyle *outByteSymbolLineStyle = [CPTMutableLineStyle lineStyle];
    outByteSymbolLineStyle.lineColor = outByteColor;
    CPTPlotSymbol *outByteSymbol = [CPTPlotSymbol starPlotSymbol];
    outByteSymbol.fill = [CPTFill fillWithColor:outByteColor];
    outByteSymbol.lineStyle = outByteSymbolLineStyle;
    outByteSymbol.size = CGSizeMake(6.0f, 6.0f);
    outBytePlot.plotSymbol = outByteSymbol;
    
}

-(void)configureAxes {
    
    // 1 - Create styles
    CPTMutableTextStyle *axisTitleStyle = [CPTMutableTextStyle textStyle];
    axisTitleStyle.color = [CPTColor whiteColor];
    axisTitleStyle.fontName = @"Helvetica-Bold";
    axisTitleStyle.fontSize = 12.0f;
    CPTMutableLineStyle *axisLineStyle = [CPTMutableLineStyle lineStyle];
    axisLineStyle.lineWidth = 2.0f;
    axisLineStyle.lineColor = [CPTColor whiteColor];
    CPTMutableTextStyle *axisTextStyle = [[CPTMutableTextStyle alloc] init];
    axisTextStyle.color = [CPTColor whiteColor];
    axisTextStyle.fontName = @"Helvetica-Bold";
    axisTextStyle.fontSize = 11.0f;
    CPTMutableLineStyle *tickLineStyle = [CPTMutableLineStyle lineStyle];
    tickLineStyle.lineColor = [CPTColor whiteColor];
    tickLineStyle.lineWidth = 2.0f;
    CPTMutableLineStyle *gridLineStyle = [CPTMutableLineStyle lineStyle];
    tickLineStyle.lineColor = [CPTColor blackColor];
    tickLineStyle.lineWidth = 1.0f;
    // 2 - Get axis set
    CPTXYAxisSet *axisSet = (CPTXYAxisSet *) self.hostView.hostedGraph.axisSet;
    // 3 - Configure x-axis
    CPTAxis *x = axisSet.xAxis;
    axisSet.xAxis.axisConstraints = [CPTConstraints constraintWithLowerOffset:0.0];
    axisSet.yAxis.axisConstraints = [CPTConstraints constraintWithLowerOffset:0.0];
    
    // Display integer value to the y-axis labels
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
    [formatter setMaximumFractionDigits:0];
    axisSet.yAxis.labelFormatter = formatter;
    
    x.title = @"TimeStamp";
    x.titleTextStyle = axisTitleStyle;
    x.titleOffset = -40.0f;
    x.labelOffset = - 25.0f;
    x.axisLineStyle = axisLineStyle;
    x.labelingPolicy = CPTAxisLabelingPolicyAutomatic;
    x.labelTextStyle = axisTextStyle;
    x.majorTickLineStyle = axisLineStyle;
    x.majorTickLength = 4.0f;
    x.tickDirection = CPTSignPositive;
    
    x.majorIntervalLength=CPTDecimalFromFloat(1.0f);
    x.minorTicksPerInterval = 0;
    x.borderWidth = 0;

    // 4 - Configure y-axis
    CPTAxis *y = axisSet.yAxis;
    y.title = @"In/out @Kbps";
    y.titleTextStyle = axisTitleStyle;
    y.titleOffset = -50.0f;
    y.axisLineStyle = axisLineStyle;
    y.majorGridLineStyle = gridLineStyle;
    y.labelingPolicy = CPTAxisLabelingPolicyAutomatic;//none to avoid auto label
    y.labelTextStyle = axisTextStyle;
    y.labelOffset = - 30.0f;
    y.majorTickLineStyle = axisLineStyle;
    y.majorTickLength = 4.0f;
    y.minorTickLength = 2.0f;
    y.majorIntervalLength=CPTDecimalFromFloat(1.0f);
    y.minorTicksPerInterval = 0;
    y.tickDirection = CPTSignPositive;
}

#pragma mark - CPTPlotDataSource methods
-(NSUInteger)numberOfRecordsForPlot:(CPTPlot *)plot {
    return [inbytes count];
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


-(NSNumber *)numberForPlot:(CPTPlot *)plot field:(NSUInteger)fieldEnum recordIndex:(NSUInteger)index {
    
    switch (fieldEnum) {
        case CPTScatterPlotFieldX:
            currentIndex = index;
            return [NSNumber numberWithUnsignedInteger:index];
            break;
            
        case CPTScatterPlotFieldY:
            if ([plot.identifier isEqual:@"inBytes"] == YES) {
              
                if([inbytes count]==0){
                    return [NSDecimalNumber zero];
                }
                return [inbytes objectAtIndex:index];
           }
            
            else if ([plot.identifier isEqual:@"outBytes"] == YES) {
                return [outbytes objectAtIndex:index];
                
            }
            break;
    }
    return [NSDecimalNumber zero];
}


//Plots the graph based on new data received.
-(void)plotGraph:(NSNotification *)notification{
    
    
    if ([[notification name] isEqualToString:@"graphData"])
    {
        
        __block NSNotification *not = notification;
        dispatch_async(dispatch_get_main_queue(), ^{
            NSDictionary *_nsmCache=[not userInfo];
            for (NSString* key in _nsmCache) {
                
                 if([key isEqual:_selectedIface]){
                    QGRCachedGraphData *nsm = [_nsmCache objectForKey:key];
                    inbytes=nsm.inData;
                    outbytes=nsm.outData;
                    
                     CPTXYPlotSpace *plotSpace = (CPTXYPlotSpace *)graph.defaultPlotSpace;
                     NSUInteger location       = (currentIndex >= xMaxCount ? currentIndex - xMaxCount : 0);
                     
                     
                     CPTPlotRange *newRange = [CPTPlotRange plotRangeWithLocation:CPTDecimalFromInteger(location)
                                                                           length:CPTDecimalFromInteger(xMaxCount + 2)];
                     
                     // Scales x and y axis
                     [plotSpace scaleToFitPlots:[graph allPlots]];
                     
                     // expand the y-axis to fit star in screen
                     CPTPlotRange *yRange = [CPTPlotRange plotRangeWithLocation:plotSpace.yRange.location
                                                                         length:CPTDecimalAdd(plotSpace.yRange.length, CPTDecimalFromInteger(3))];
                     plotSpace.yRange = yRange;
                     
                     // reset the x-axis to fixed range
                     plotSpace.xRange = newRange;
                     
                     // plot
                     [self.hostView.hostedGraph reloadData];
                }
                
            }
            not=nil;
            _nsmCache=nil;
        });
    }
    
}

- (void)setSelectedIface:(id)newSelectedIface
{
    if (_selectedIface != newSelectedIface) {
        _selectedIface = newSelectedIface;
        
    }
}


-(void)dealloc
{
    // Remove listeners
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}
@end
