# plot.gp
if (ARG1 eq "") {
    print "Usage: gnuplot plot.gp <datafile> <title> <outputfile>"
    exit
}

if (ARG2 eq "") {
    title = "Default Title"
} else {
    title = ARG2
}

if (ARG3 eq "") {
    output = "output.png"
} else {
    output = ARG3
}

set terminal png
set output output

set title title

set xlabel "X-axis"
set ylabel "Y-axis"

plot ARG1 using 1:2 with lines title title
