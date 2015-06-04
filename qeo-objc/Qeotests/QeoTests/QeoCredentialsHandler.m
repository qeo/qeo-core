#import "QeoCredentialsHandler.h"

@implementation QeoCredentialsHandler

+ (void)loadQeoCredentials
{
    NSString *libraryDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) lastObject];
    NSString *dotQeoDirPath = [libraryDir stringByAppendingPathComponent:@"/.qeo"];

    NSLog(@"Current .qeo directory path: %@",dotQeoDirPath);

    NSError *error;
    NSFileManager* fileManager = [NSFileManager defaultManager];
    if(YES == [fileManager fileExistsAtPath:dotQeoDirPath]) {
        [QeoCredentialsHandler removeQeoCredentials];
    }

    // Create .qeo dir
    [fileManager createDirectoryAtPath:dotQeoDirPath withIntermediateDirectories:NO attributes:nil error:nil];

    // Copy files
    NSBundle *unitTestBundle = [NSBundle bundleForClass:[self class]];
    if (NO == [fileManager copyItemAtPath:[unitTestBundle pathForResource:@"7b3da75059501287_policy" ofType:@"mime"]
                                   toPath:[NSString stringWithFormat:@"%@/7b3da75059501287_policy.mime",dotQeoDirPath]
                                    error:&error]) {
        NSLog(@"Failed to Copy file: 7b3da75059501287_policy.mime; %@",error.description);
    }
    if (NO == [fileManager copyItemAtPath:[unitTestBundle pathForResource:@"truststore" ofType:@"p12"]
                                   toPath:[NSString stringWithFormat:@"%@/truststore.p12",dotQeoDirPath]
                                    error:&error]) {
        NSLog(@"Failed to Copy file: truststore.p12; %@",error.description);
    }
    if (NO == [fileManager copyItemAtPath:[unitTestBundle pathForResource:@"url" ofType:nil]
                                   toPath:[NSString stringWithFormat:@"%@/url",dotQeoDirPath]
                                    error:&error]) {
        NSLog(@"Failed to Copy file: url; %@",error.description);
    }
}

+ (void)removeQeoCredentials
{
    NSString *libraryDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) lastObject];
    NSString *dotQeoDirPath = [libraryDir stringByAppendingPathComponent:@"/.qeo"];

    NSLog(@"Current .qeo directory path: %@",dotQeoDirPath);

    NSError *error;
    NSFileManager* fileManager = [NSFileManager defaultManager];
    if (YES == [fileManager fileExistsAtPath:dotQeoDirPath]) {
        // Remove all files if any
        for (NSString *file in [fileManager contentsOfDirectoryAtPath:dotQeoDirPath error:&error]) {
            BOOL success = [fileManager removeItemAtPath:[NSString stringWithFormat:@"%@/%@",dotQeoDirPath,file] error:&error];
            if (NO == success) {
                NSLog(@"Could not remove file: %@", [NSString stringWithFormat:@"%@/%@",dotQeoDirPath,file]);
            } else {
                NSLog(@"Removed file: %@", [NSString stringWithFormat:@"%@/%@",dotQeoDirPath,file]);
            }
        }
        [fileManager removeItemAtPath:dotQeoDirPath error:&error];
    }
}

@end
