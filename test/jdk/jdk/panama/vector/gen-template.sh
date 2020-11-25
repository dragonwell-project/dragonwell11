#!/bin/bash
#
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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
# or visit www.oracle.com if you need additional information or have
# questions.
#

TEMPLATE_FOLDER="templates/"
generate_perf_tests=true

unary="Unary-op"
unary_masked="Unary-Masked-op"
unary_scalar="Unary-Scalar-op"
ternary="Ternary-op"
ternary_masked="Ternary-Masked-op"
ternary_scalar="Ternary-Scalar-op"
binary="Binary-op"
binary_masked="Binary-Masked-op"
binary_scalar="Binary-Scalar-op"
blend="Blend-op"
compare_template="Compare"
reduction_scalar="Reduction-Scalar-op"
reduction_template="Reduction-op"
reduction_min_template="Reduction-Scalar-Min-op"
reduction_max_template="Reduction-Scalar-Max-op"
unary_math_template="Unary-op-math"
binary_math_template="Binary-op-math"
bool_reduction_scalar="BoolReduction-Scalar-op"
bool_reduction_template="BoolReduction-op"
with_op_template="With-Op"
shift_template="Shift-op"
shift_masked_template="Shift-Masked-op"
gather_template="Gather-op"
scatter_template="Scatter-op"
get_template="Get-op"
rearrange_template="Rearrange"

function replace_variables {
  local filename=$1
  local output=$2
  local kernel=$3
  local test=$4
  local op=$5
  local init=$6
  local guard=$7
  local masked=$8
  local op_name=$9

  if [ "x${kernel}" != "x" ]; then
    local kernel_escaped=$(echo -e "$kernel" | tr '\n' '|')
    sed "s/\[\[KERNEL\]\]/${kernel_escaped}/g" $filename > ${filename}.current1
    cat ${filename}.current1 | tr '|' "\n" > ${filename}.current
    rm "${filename}.current1"
  else
    cp $filename ${filename}.current
  fi

  sed -i -e "s/\[\[TEST\]\]/${test}/g" ${filename}.current
  sed -i -e "s/\[\[TEST_TYPE\]\]/${masked}/g" ${filename}.current
  sed -i -e "s/\[\[TEST_OP\]\]/${op}/g" ${filename}.current
  sed -i -e "s/\[\[TEST_INIT\]\]/${init}/g" ${filename}.current
  sed -i -e "s/\[\[OP_NAME\]\]/${op_name}/g" ${filename}.current

  # Guard the test if necessary
  if [ "$guard" != "" ]; then
    echo -e "#if[${guard}]\n" >> $output
  fi
  cat "${filename}.current" >> $output
  if [ "$guard" != "" ]; then
    echo -e "#end[${guard}]\n" >> $output
  fi

  rm ${filename}.current
}

function gen_op_tmpl {
  local template=$1
  local test=$2
  local op=$3
  local unit_output=$4
  local perf_output=$5
  local perf_scalar_output=$6
  local guard=""
  local init=""
  if [ $# -gt 6 ]; then
    guard=$7
  fi
  if [ $# == 8 ]; then
    init=$8
  fi

  local masked=""
  if [[ $template == *"Masked"* ]]; then
    masked="Masked"
  fi

  local op_name=""
  if [[ $template == *"Shift"* ]]; then
    op_name="Shift"
  elif [[ $template == *"Get"* ]]; then
    op_name="extract"
  fi

  local unit_filename="${TEMPLATE_FOLDER}/Unit-${template}.template"
  local kernel_filename="${TEMPLATE_FOLDER}/Kernel-${template}.template"
  local perf_wrapper_filename="${TEMPLATE_FOLDER}/Perf-wrapper.template"
  local perf_vector_filename="${TEMPLATE_FOLDER}/Perf-${template}.template"
  local perf_scalar_filename="${TEMPLATE_FOLDER}/Perf-Scalar-${template}.template"

  local kernel=""
  if [ -f $kernel_filename ]; then
    kernel="$(cat $kernel_filename)"
  fi

  # Replace template variables in both unit and performance test files (if any)
  replace_variables $unit_filename $unit_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name"

  if [ -f $perf_vector_filename ]; then
    replace_variables $perf_vector_filename  $perf_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name"
  elif [ -f $kernel_filename ]; then
    replace_variables $perf_wrapper_filename $perf_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name"
  elif [[ $template != *"-Scalar-"* ]] && [[ $template != "Get-op" ]] && [[ $template != "With-Op" ]]; then
    echo "Warning: missing perf: $@"
  fi

  if [ -f $perf_scalar_filename ]; then
    replace_variables $perf_scalar_filename $perf_scalar_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name"
  elif [[ $template != *"-Scalar-"* ]] && [[ $template != "Get-op" ]] && [[ $template != "With-Op" ]]; then
    echo "Warning: Missing PERF SCALAR: $perf_scalar_filename"
  fi
}

function gen_binary_alu_op {
  echo "Generating binary op $1 ($2)..."
  gen_op_tmpl $binary "$@"
  gen_op_tmpl $binary_masked "$@"
}

function gen_shift_cst_op {
  echo "Generating Shift constant op $1 ($2)..."
  gen_op_tmpl $shift_template "$@"
  gen_op_tmpl $shift_masked_template "$@"
}

function gen_unary_alu_op {
  echo "Generating unary op $1 ($2)..."
  gen_op_tmpl $unary_scalar "$@"
  gen_op_tmpl $unary "$@"
  gen_op_tmpl $unary_masked "$@"
}

function gen_ternary_alu_op {
  echo "Generating ternary op $1 ($2)..."
  gen_op_tmpl $ternary_scalar "$@"
  gen_op_tmpl $ternary "$@"
  gen_op_tmpl $ternary_masked "$@"
}

function gen_binary_op {
  echo "Generating binary op $1 ($2)..."
#  gen_op_tmpl $binary_scalar "$@"
  gen_op_tmpl $binary "$@"
}

function gen_reduction_op {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_scalar "$@"
  gen_op_tmpl $reduction_template "$@"
}

function gen_reduction_op_min {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_min_template "$@"
  gen_op_tmpl $reduction_template "$@"
}

function gen_reduction_op_max {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_max_template "$@"
  gen_op_tmpl $reduction_template "$@"
}

function gen_bool_reduction_op {
  echo "Generating boolean reduction op $1 ($2)..."
  gen_op_tmpl $bool_reduction_scalar "$@"
  gen_op_tmpl $bool_reduction_template "$@"
}

function gen_with_op {
  echo "Generating with op $1 ($2)..."
  gen_op_tmpl $with_op_template "$@"
}

function gen_get_op {
  echo "Generating get op $1 ($2)..."
  gen_op_tmpl $get_template "$@"
}

function gen_unit_header {
  cat $TEMPLATE_FOLDER/Unit-header.template > $1
}

function gen_unit_footer {
  cat $TEMPLATE_FOLDER/Unit-footer.template >> $1
}

function gen_perf_header {
  cat $TEMPLATE_FOLDER/Perf-header.template > $1
}

function gen_perf_footer {
  cat $TEMPLATE_FOLDER/Perf-footer.template >> $1
}

function gen_perf_scalar_header {
  cat $TEMPLATE_FOLDER/Perf-Scalar-header.template > $1
}

function gen_perf_scalar_footer {
  cat $TEMPLATE_FOLDER/Perf-Scalar-footer.template >> $1
}
unit_output="unit_tests.template"
perf_output="perf_tests.template"
perf_scalar_output="perf_scalar_tests.template"
gen_unit_header $unit_output
gen_perf_header $perf_output
gen_perf_scalar_header $perf_scalar_output

# ALU binary ops.
gen_binary_alu_op "add" "a + b" $unit_output $perf_output $perf_scalar_output
gen_binary_alu_op "sub" "a - b" $unit_output $perf_output $perf_scalar_output
gen_binary_alu_op "div" "a \/ b" $unit_output $perf_output $perf_scalar_output "FP"
gen_binary_alu_op "mul" "a \* b" $unit_output $perf_output $perf_scalar_output
gen_binary_alu_op "and" "a \& b" $unit_output $perf_output $perf_scalar_output "BITWISE"
gen_binary_alu_op "or" "a | b" $unit_output $perf_output $perf_scalar_output "BITWISE"
gen_binary_alu_op "xor" "a ^ b" $unit_output $perf_output $perf_scalar_output "BITWISE"

# Shifts
gen_binary_alu_op "shiftR" "(a >>> b)" $unit_output $perf_output $perf_scalar_output "intOrLong"
gen_binary_alu_op "shiftL" "(a << b)" $unit_output $perf_output $perf_scalar_output "intOrLong"
gen_binary_alu_op "aShiftR" "(a >> b)" $unit_output $perf_output $perf_scalar_output "intOrLong"
gen_shift_cst_op "aShiftR" "(a >> b)" $unit_output $perf_output $perf_scalar_output "BITWISE"
gen_shift_cst_op "shiftR" "(a >>> b)" $unit_output $perf_output $perf_scalar_output "BITWISE"
gen_shift_cst_op "shiftL" "(a << b)" $unit_output $perf_output $perf_scalar_output "BITWISE"

# Masked reductions.
gen_binary_op "max" "(a > b) ? a : b" $unit_output $perf_output $perf_scalar_output
gen_binary_op "min" "(a < b) ? a : b" $unit_output $perf_output $perf_scalar_output

# Reductions.
gen_reduction_op "andAll" "\&" $unit_output $perf_output $perf_scalar_output "BITWISE" "-1"
gen_reduction_op "orAll" "|" $unit_output $perf_output $perf_scalar_output "BITWISE" "0"
gen_reduction_op "xorAll" "^" $unit_output $perf_output $perf_scalar_output "BITWISE" "0"
gen_reduction_op "addAll" "+" $unit_output $perf_output $perf_scalar_output "" "0"
gen_reduction_op "subAll" "-" $unit_output $perf_output $perf_scalar_output "" "0"
gen_reduction_op "mulAll" "*" $unit_output $perf_output $perf_scalar_output "" "1"
gen_reduction_op_min "minAll" "" $unit_output $perf_output $perf_scalar_output "" "\$Wideboxtype\$.MAX_VALUE"
gen_reduction_op_max "maxAll" "" $unit_output $perf_output $perf_scalar_output "" "\$Wideboxtype\$.MIN_VALUE"

# Boolean reductions.
gen_bool_reduction_op "anyTrue" "|" $unit_output $perf_output $perf_scalar_output "BITWISE" "false"
gen_bool_reduction_op "allTrue" "\&" $unit_output $perf_output $perf_scalar_output "BITWISE" "true"

#Insert
gen_with_op "with" "" $unit_output $perf_output $perf_scalar_output "" ""

# Compares
gen_op_tmpl $compare_template "lessThan" "<" $unit_output $perf_output $perf_scalar_output
gen_op_tmpl $compare_template "greaterThan" ">" $unit_output $perf_output $perf_scalar_output
gen_op_tmpl $compare_template "equal" "==" $unit_output $perf_output $perf_scalar_output
gen_op_tmpl $compare_template "notEqual" "!=" $unit_output $perf_output $perf_scalar_output
gen_op_tmpl $compare_template "lessThanEq" "<=" $unit_output $perf_output $perf_scalar_output
gen_op_tmpl $compare_template "greaterThanEq" ">=" $unit_output $perf_output $perf_scalar_output

# Blend.
gen_op_tmpl $blend "blend" "" $unit_output $perf_output $perf_scalar_output

# Rearrange
gen_op_tmpl $rearrange_template "rearrange" "" $unit_output $perf_output $perf_scalar_output

# Get
gen_get_op "" "" $unit_output $perf_output $perf_scalar_output

# Math
gen_op_tmpl $unary_math_template "sin" "Math.sin((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "exp" "Math.exp((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "log1p" "Math.log1p((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "log" "Math.log((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "log10" "Math.log10((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "expm1" "Math.expm1((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "cos" "Math.cos((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "tan" "Math.tan((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "sinh" "Math.sinh((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "cosh" "Math.cosh((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "tanh" "Math.tanh((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "asin" "Math.asin((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "acos" "Math.acos((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "atan" "Math.atan((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $unary_math_template "cbrt" "Math.cbrt((double)a)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $binary_math_template "hypot" "Math.hypot((double)a, (double)b)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $binary_math_template "pow" "Math.pow((double)a, (double)b)" $unit_output $perf_output $perf_scalar_output "FP"
gen_op_tmpl $binary_math_template "atan2" "Math.atan2((double)a, (double)b)" $unit_output $perf_output $perf_scalar_output "FP"

# Ternary operations.
gen_ternary_alu_op "fma" "Math.fma(a, b, c)" $unit_output $perf_output $perf_scalar_output "FP"

# Unary operations.
gen_unary_alu_op "neg" "-((\$type\$)a)" $unit_output $perf_output $perf_scalar_output
gen_unary_alu_op "abs" "Math.abs((\$type\$)a)" $unit_output $perf_output $perf_scalar_output
gen_unary_alu_op "not" "~((\$type\$)a)" $unit_output $perf_output $perf_scalar_output "BITWISE"
gen_unary_alu_op "sqrt" "Math.sqrt((double)a)" $unit_output $perf_output $perf_scalar_output "FP"

# Gather Scatter operations.
gen_op_tmpl $gather_template "gather" "" $unit_output $perf_output $perf_scalar_output "!byteOrShort"
gen_op_tmpl $scatter_template "scatter" "" $unit_output $perf_output $perf_scalar_output "!byteOrShort"

gen_unit_footer $unit_output
gen_perf_footer $perf_output
gen_perf_scalar_footer $perf_scalar_output

rm templates/*.current*
