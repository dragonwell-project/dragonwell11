#!/usr/bin/perl -w
# Copyright (c) 2024 Alibaba Group Holding Limited. All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#usage: perl -w ${TESTSRC}/get-native-memory-usage.pl 25 "Code-malloc:2.5,Code-mmap:2.8,Compiler-malloc:4.6" `ls *-native_memory-summary.log | sort -n | xargs`
use strict;
use warnings;
use POSIX;
use File::Path qw(make_path);
my $verbose = 0;

die "please input split number and more than 3 jcmd native log files" if( @ARGV < 10 );
my $split = shift(@ARGV);
my $rules = shift(@ARGV);
my $baseline = parserJcmdResult(shift(@ARGV));
my @nameArray;
my %resultCsv;
my %resultMaxValue;
my %resultMaxIndex;
my %resultMinValue;
my %resultMinIndex;
my %resultQuarterValue;
my %resultThirdValue;
my %resultHalfValue;
my %resultLastValue;
my %isIncreasementalResultHash;
my $memoryLeakNumber = 0;
my $plotDataDir = "plot-data";
my $lastFile = $ARGV[-1];
$lastFile =~ /^([0-9]+)-.*?/;
my $lastIndex = $1;
my $quarterIndex = ceil($lastIndex / 4);
my $thirdIndex = ceil($lastIndex / 3);
my $halfIndex = ceil($lastIndex / 2);
die "lastIndex undefine!" if( ! defined $lastIndex );

foreach my $key ( sort keys %$baseline )
{
    my $value = $baseline->{$key};
    print("first line : $key : $value\n") if( $verbose > 1 );
    push @nameArray, $key;
    $resultCsv{$key} = "$key" . "," . "$value";
    $resultMaxIndex{$key} = 0;
    $resultMaxValue{$key} = $value;
    $resultMinIndex{$key} = 0;
    $resultMinValue{$key} = $value;
}
foreach my $file ( @ARGV )
{
    $file =~ /^([0-9]+)-.*?/;
    my $index = $1;
    die "index undefine!" if( ! defined $index );
    my $data = parserJcmdResult($file);
    foreach my $key ( sort @nameArray )
    {
        my $value = $data->{$key};
        print("$index : $key : $value\n") if( $verbose > 1 );
        $resultCsv{$key} = $resultCsv{$key} . "," . "$value";
        if( $value > $resultMaxValue{$key} )
        {
            $resultMaxIndex{$key} = $index;
            $resultMaxValue{$key} = $value;
        }
        if( $value < $resultMinValue{$key} )
        {
            $resultMinIndex{$key} = $index;
            $resultMinValue{$key} = $value;
        }
        if( $index == $quarterIndex )
        {
            $resultQuarterValue{$key} = $value;
        }
        if( $index == $thirdIndex )
        {
            $resultThirdValue{$key} = $value;
        }
        if( $index == $halfIndex )
        {
            $resultHalfValue{$key} = $value;
        }
        if( $index == $lastIndex )
        {
            $resultLastValue{$key} = $value;
        }
    }
}

if( ! -d $plotDataDir )
{
    make_path($plotDataDir);
}

open(my $csvFh, ">native-memory-summary.csv");
open(my $summaryFh, ">native-memory-summary.txt");
print $summaryFh ("total $lastIndex files, quarter index is $quarterIndex, third index is $thirdIndex, half index is $halfIndex.\n");
foreach my $key ( sort @nameArray )
{
    my @data = split /,/, $resultCsv{$key};
    my $name = shift(@data);
    die "key=$key != name=$name" if( $name ne $key );

    print $csvFh "$resultCsv{$key}\n";
    my $maxMultiple = sprintf("%.1f", $resultMaxValue{$key} / $resultMinValue{$key});
    my $quarterMultiple = sprintf("%.1f", $resultLastValue{$key} / $resultQuarterValue{$key});
    my $thirdMultiple = sprintf("%.1f", $resultLastValue{$key} / $resultThirdValue{$key});
    my $halfMultiple = sprintf("%.1f", $resultLastValue{$key} / $resultHalfValue{$key});
    my $thirdSurprise = "";
    my $isIncreasementalResult = isIncreasemental($name, @data);
    $isIncreasementalResultHash{$name} = $isIncreasementalResult;
    my $isMemoryLeak = "";
    if( $thirdMultiple >= 2.5 )
    {
        $thirdSurprise = "!!";
        if( $isIncreasementalResult == 0 )
        {
            $isMemoryLeak = "\tMemoryLeak!!!"
        }
    }
    print $summaryFh "$key\tmax=$resultMaxValue{$key},index=$resultMaxIndex{$key}\tmin=$resultMinValue{$key},index=$resultMinIndex{$key}\tquarter=$resultQuarterValue{$key},third=$resultThirdValue{$key},half=$resultHalfValue{$key}\tmax/min=$maxMultiple,last/quarter=$quarterMultiple,last/half=$halfMultiple,last/third=$thirdMultiple$thirdSurprise\tisIncreasemental=$isIncreasementalResult$isMemoryLeak\n";

    #write plot data
    my $i = 0;
    open(my $fh, ">$plotDataDir/$name.txt");
    foreach my $value ( @data )
    {
        print $fh "$i $value\n";
        $i++;
    }
    close($fh);
}
close($csvFh);
close($summaryFh);


my $lastIndexResultHash = parserJcmdResult($ARGV[-1]);
my $thirdIndexResultHash = parserJcmdResult($ARGV[ceil(scalar(@ARGV)/3)]);
foreach my $rule ( split /,/, $rules )
{
    print("rule: $rule\n");
    my($moduleName, $coefficient) = split /:/, $rule;
    print("$moduleName: $coefficient\n") if( $verbose > 3 );
    my $lastIndexValue = $lastIndexResultHash->{$moduleName};
    my $thirdIndexValue = $thirdIndexResultHash->{$moduleName};
    die "can't find $moduleName memory usage information!" if( ! defined $lastIndexValue );
    die "can't find $moduleName memory usage information!" if( ! defined $thirdIndexValue );
    my $compareValue = $thirdIndexValue * $coefficient;
    if( $lastIndexValue > $compareValue && $isIncreasementalResultHash{$moduleName} == 0 )
    {
        warn("$moduleName: $lastIndexValue > $compareValue=$thirdIndexValue*$coefficient");
        $memoryLeakNumber++;
    }
}
if( $memoryLeakNumber > 0 )
{
    die "memoryLeakNumber=$memoryLeakNumber!";
}



sub parserJcmdResult
{
    my ($filename) = @_;
    my %malloc;
    my $name;
    my $number;
    open(my $fh, "<$filename") or die "Can't open file '$filename' $!";
    foreach my $line ( <$fh> )
    {
        chomp($line);
        if( $line =~ /^-\s*(.*)\s+\(/ )
        {
            $name = $1;
            $name =~ s/\s+//g;
            $number = -1;
            next;
        }
        if( $line =~ /\(malloc=([0-9]+)KB/ )
        {
            $number = $1;
            die "filename=$filename\tline=$line can't get name!\n" if( length($name) <= 0 );
            my $key = "$name" . "-malloc";
            print("name=$key\t\tnumber=$number\n") if( $verbose == 1 );
            if( $number == 0 )
            {
                if( $verbose > 0 )
                {
                    warn("$key value is 0");
                }
                $number = 0.01;
            }
            $malloc{$key} = $number;
            next;
        }
        if( $line =~ /\(mmap:.*committed=([0-9]+)KB/ )
        {
            $number = $1;
            die "filename=$filename\tline=$line can't get name!\n" if( length($name) <= 0 );
            my $key = "$name" . "-mmap";
            print("name=$key\t\tnumber=$number\n") if( $verbose == 1 );
            if( $number == 0 )
            {
                if( $verbose > 0 )
                {
                    warn("$key value is 0");
                }
                $number = 1;
            }
            $malloc{$key} = $number;
            next;
        }
    }
    close($fh);
    return \%malloc;
};

sub isIncreasemental
{
    my $name = shift(@_);
    my @array = @_;
    my $length = scalar(@array);
    my $windowLength = floor($length/$split);
    warn("$name: windowLength=$windowLength\n") if( $verbose > 0 );
    my $count = $windowLength * $split;
    warn("$name: count=$count, $length=$length\n")  if( $verbose > 0 );;
    my $previousSum = 0;
    my $steady = 0;
    my $result = 0;

    #calculate the main part data
    foreach my $i ( 0..$split-1 )
    {
        my $currentSum = 0;
        foreach my $j (0..$windowLength-1)
        {
            my $index = $i*$windowLength+$j;
            $currentSum += $array[$i*$windowLength+$j];
        }
        $currentSum /= $windowLength;
        warn("$name: currentSum=$currentSum, previousSum=$previousSum\n") if( $verbose >= 3 );
        if( $currentSum < $previousSum )
        {
            $result++;
            warn("$name: currentSum=$currentSum, previousSum=$previousSum\n") if( $verbose >= 1 );
        }
        elsif( $currentSum == $previousSum )
        {
            $steady++;
        }
        $previousSum = $currentSum;
    }

    # warn("$name: steady=$steady, split=$split\n") if( $verbose >= 0 );
    #calculate the tail data
    if( ($length-$count) != 0 )
    {
        my $currentSum = 0;
        foreach my $i ( $count .. ($length-1) )
        {
            $currentSum += $array[$i];;
        }
        $currentSum /= ($length-$count);
        if( $currentSum < $previousSum )
        {
            $result++;
            warn("$name: currentSum=$currentSum, previousSum=$previousSum\n") if( $verbose >= 1 );
        }
        elsif( $currentSum == $previousSum || abs($currentSum-$previousSum) > 0.1 )
        {
            $steady++;
        }
    }
    else
    {
        warn("$name: tail count is zero.\n") if( $verbose >= 1 );
    }

    #statistics the result
    warn("$name: steady=$steady, split=$split\n") if( $verbose >= 2 );
    if( $steady == $split )
    {
        $result = -1;
    }
    return $result;
}
