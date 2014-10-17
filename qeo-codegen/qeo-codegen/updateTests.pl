#!/usr/bin/perl
# Do not use this script directly, instead run gradle generateTestCode
use strict;

my $version = "0.20.0-SNAPSHOT";

sub generate($$)
{
	my ($folder, $qdm) = @_;
	my $jar = "build/libs/qeo-codegen-$version-executable.jar";
    print "Generating C files for $qdm\n";
	`java -jar $jar -lc -o $folder/c $qdm`;
	print "Generating java files for $qdm\n";
	`java -jar $jar -ljava -o $folder/java $qdm`;
	print "Generating js files for $qdm\n";
    `java -jar $jar -ljs -o $folder/js $qdm`;
    print "Generating ObjectiveC files for $qdm\n";
	`java -jar $jar -lobjectivec -o $folder/objectivec $qdm`;
}

sub getQdms($)
{
	my ($folder) = @_;
	foreach (`ls "$folder/qdm"`) {
		chomp();
		my $file = $_;
		my $qdm = "$folder/qdm/$file";
		if (-f $qdm && ! -l $qdm) {
			generate($folder, $qdm);
		}
	}
}

sub getTests() 
{
	foreach (`ls test`) {
		chomp();
		my $test = "test/" . $_;
		if (-d $test) {
			print "\n" . $test . "\n";
			getQdms($test);
		}
	}
}

sub main()
{
	getTests()
}

main();

