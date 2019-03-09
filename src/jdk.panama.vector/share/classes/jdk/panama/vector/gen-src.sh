#!/bin/bash

javac -d . ../../../../../../../make/jdk/src/classes/build/tools/spp/Spp.java

SPP=build.tools.spp.Spp

typeprefix=

for type in byte short int long float double
do
  Type="$(tr '[:lower:]' '[:upper:]' <<< ${type:0:1})${type:1}"
  TYPE="$(tr '[:lower:]' '[:upper:]' <<< ${type})"
  args="-K$type -Dtype=$type -DType=$Type -DTYPE=$TYPE"

  Boxtype=$Type
  Wideboxtype=$Boxtype

  kind=BITWISE

  bitstype=$type
  Bitstype=$Type
  Boxbitstype=$Boxtype

  fptype=$type
  Fptype=$Type
  Boxfptype=$Boxtype

  case $type in
    byte)
      Wideboxtype=Integer
      sizeInBytes=1
      args="$args -KbyteOrShort"
      ;;
    short)
      Wideboxtype=Integer
      sizeInBytes=2
      args="$args -KbyteOrShort"
      ;;
    int)
      Boxtype=Integer
      Wideboxtype=Integer
      fptype=float
      Fptype=Float
      Boxfptype=Float
      sizeInBytes=4
      args="$args -KintOrLong -KintOrFP -KintOrFloat"
      ;;
    long)
      fptype=double
      Fptype=Double
      Boxfptype=Double
      sizeInBytes=8
      args="$args -KintOrLong -KlongOrDouble"
      ;;
    float)
      kind=FP
      bitstype=int
      Bitstype=Int
      Boxbitstype=Integer
      sizeInBytes=4
      args="$args -KintOrFP -KintOrFloat"
      ;;
    double)
      kind=FP
      bitstype=long
      Bitstype=Long
      Boxbitstype=Long
      sizeInBytes=8
      args="$args -KintOrFP -KlongOrDouble"
      ;;
  esac

  args="$args -K$kind -DBoxtype=$Boxtype -DWideboxtype=$Wideboxtype"
  args="$args -Dbitstype=$bitstype -DBitstype=$Bitstype -DBoxbitstype=$Boxbitstype"
  args="$args -Dfptype=$fptype -DFptype=$Fptype -DBoxfptype=$Boxfptype"

  abstractvectortype=${typeprefix}${Type}Vector
  abstractbitsvectortype=${typeprefix}${Bitstype}Vector
  abstractfpvectortype=${typeprefix}${Fptype}Vector
  args="$args -Dabstractvectortype=$abstractvectortype -Dabstractbitsvectortype=$abstractbitsvectortype -Dabstractfpvectortype=$abstractfpvectortype"
  echo $args
  java $SPP -nel $args \
    < X-Vector.java.template \
    > $abstractvectortype.java

  if [ VAR_OS_ENV==windows.cygwin ]; then
    tr -d '\r' < $abstractvectortype.java > temp
    mv temp $abstractvectortype.java
  fi

  java $SPP -nel $args \
    < X-VectorHelper.java.template \
    > ${abstractvectortype}Helper.java

  if [[ "x${VAR_OS_ENV}" == "xwindows.cygwin" ]]; then
    tr -d '\r' < ${abstractvectortype}Helper.java > temp
    mv temp ${abstractvectortype}Helper.java
  fi

  old_args="$args"
  for bits in 64 128 256 512 Max
  do
    vectortype=${typeprefix}${Type}${bits}Vector
    masktype=${typeprefix}${Type}${bits}Mask
    shuffletype=${typeprefix}${Type}${bits}Shuffle
    bitsvectortype=${typeprefix}${Bitstype}${bits}Vector
    fpvectortype=${typeprefix}${Fptype}${bits}Vector
    vectorindexbits=$((bits * 4 / sizeInBytes))
    if [[ "${bits}" == "Max" ]]; then
        vectorindextype="vix.getClass()"
    else
        vectorindextype="Int${vectorindexbits}Vector.class"
    fi;

    shape=S${bits}Bit
    Shape=S_${bits}_BIT
    args="$old_args"
    if [[ "${vectortype}" == "Long64Vector" || "${vectortype}" == "Double64Vector" ]]; then
      args="$args -KlongOrDouble64"
    fi
    bitargs="$args -Dbits=$bits -Dvectortype=$vectortype -Dmasktype=$masktype -Dshuffletype=$shuffletype -Dbitsvectortype=$bitsvectortype -Dfpvectortype=$fpvectortype -Dvectorindextype=$vectorindextype -Dshape=$shape -DShape=$Shape"

    echo $bitargs
    java $SPP -nel $bitargs \
      < X-VectorBits.java.template \
      > $vectortype.java

    if [[ "x${VAR_OS_ENV}" == "xwindows.cygwin" ]]; then
      tr -d  '\r' < $vectortype.java > temp
      mv temp $vectortype.java
    fi
  done

done

rm -fr build

