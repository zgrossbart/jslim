JSlim - It's your favorite JavaScript library, only faster
==================================================

JSlim is a JavaScript optimizer based on the [Google Closure Compiler](http://code.google.com/closure/compiler/), but instead of optimizing your code it optimizes the code you use.  

Most websites use JavaScript libraries like [JQuery](http://jquery.com/) or [Prorotype](http://www.prototypejs.org/), but they don't use the whole library.  JSlim builds you a new version of your favorite JavaScript library with only the code you're using.

Building JSlim
--------------------------------------

The project depends on the [Google Closure Compiler](http://code.google.com/closure/compiler/) and it can't run with the version from Maven central.  You must download the source for this compiler and reference the built compiler.jar file in the build.gradle file.  I'm working on making this easier.

This project builds with [Gradle](http://www.gradle.org).  Build the application by running gradle in the root directory.

This project builds and runs on Windows, Mac, and Linux.

Using JSlim
--------------------------------------

<pre><code>
java JSlimRunner [options...] arguments...

 --charset VAL                          : Input and output charset for all files. By default, we accept UTF-8 as input and output US_ASCII
 --combine_files                        : Pass this argument to combine the library files and the regular files into a single output file.
 --compilation_level [WHITESPACE_ONLY | : Specifies the compilation level to use. 
  SIMPLE_OPTIMIZATIONS | ADVANCED_OPTIM : Options: WHITESPACE_ONLY, SIMPLE_OPT IMIZATIONS, ADVANCED_OPTIMIZATIONS
 IZATIONS | NONE]                       
 --externs VAL                          : The file containing javascript externs. You may specify multiple.
 --flagfile VAL                         : A file containing additional command-line options.
 --help                                 : Displays this message
 --js VAL                               : The javascript filename. You may specify multiple
 --js_output_file VAL                   : Primary output filename. If not specified, output is written to stdout
 --lib_js VAL                           : The javascript library filename. You may specify multiple
 --logging_level [ALL | CONFIG | FINE | : The logging level (standard java.util.logging.Level values) for Compiler progress. Does not control errors or warnings for the JavaScript code under compilation
  FINER | FINEST | INFO | OFF | SEVERE  
 | WARNING]                              
 --no_validate                          : Pass this argument to skip the pre-parse file validation step.  This is faster, but won't provide good error messages if the input files are invalid JavaScript.
 --print_tree                           : Prints out the parse tree and exits
 --skip_gzip                            : Skip GZIPing the results

</code></pre>

This repository includes a number of sample application you can use to try JSlim with.  The easiest place to start is with a simple JQuery/userscore.js.  You can slim it like this:

        build/install/jslim/bin/jslim --js_output_file out.js --js main.js --lib_js libs/jquery-1.6.2.js --lib_js libs/underscore.js

How JSlim works
--------------------------------------

JavaScript organizes blocks of code into functions.  Some functions are named ans stand alone like this:

<pre><code>
function myFunction() {
    alert('hey there');
}
</code></pre>

Other functions are organized into JavaScript closures like this:

<pre><code>
var myObj = {
    myFunction: function() {
        alert('hey there');
    }
};
</code></pre>

JSlim figures out which functions your code is calling and which functions they're calling.  Then it removes all of the functions which aren't being used.  

JSlim can track most function calls, but there are some where it can't follow what you're calling.  The easiest way to break JSlim is using the [eval](http://en.wikipedia.org/wiki/Eval#JavaScript) function like this:

<pre><code>
function myFunction() {
    alert('hey there');
}

eval('myfunction();');
</code></pre>

JSlim can't follow this function call to `myFunction` since it is part of an evaluated string.  In this case you must reference `myFunction` as an external reference.  Many JavaScript library do dynamic loading like this, especially for effects.

How much does JSlim save?
--------------------------------------

JSlim works very differently depending on the library it is slimming down.  The more a library uses named functions with discrete blocks and clean hierarchy the more we can slim it down.  

JSlim doesn't work well with libraries uses mostly anonymous functions and other constructs which are difficult to track down.  JSlim is also strictly a [static code analysis](http://en.wikipedia.org/wiki/Static_code_analysis) tool which means it doesn't run any JavaScript code.  

For example, if your code looks like this:

<pre><code>
if (value === 1) {
    alert($.now());
} else if (value === 2) {
    $.noop();
}
</code></pre>

In this case JSlim will keep the `now` function and the `noop` function since it can't tell what values might be passed.  

JSlim is with a simple JQuery/underscore.js application where it reduces the total size of the libraries by 26%.  Most libraries are reduced by 10-20 percent, but the savings in total size is only part of the story.

Most JavaScript compressors take your JavaScript and make it smaller by removing whitespace and comments and inlining functions.  JSlim actually removes code which reduces [computational complexity](http://en.wikipedia.org/wiki/Computational_complexity_theory).  The means your libraries aren't just smaller, but they actually run faster.

How stable is JSlim?
--------------------------------------

The short answer is that JSlim isn't very stable at all yet.  You still need to build it yourself and it requires a development version of the [Google Closure Compiler](http://code.google.com/closure/compiler/).  I've tested JSlim with the following libraries:

* [JQuery](http://jquery.com/)
* [JQuery UI](http://jqueryui.com/)
* [Modernizr](http://www.modernizr.com/)
* [Mootools](http://mootools.net/)
* [Prototype](http://www.prototypejs.org/)
* [Raphaël](http://raphaeljs.com/)
* [underscore.js](http://documentcloud.github.com/underscore/)

JSlim roadmap
--------------------------------------

In the short term I'm focusing on improvements to the core compilation process.  I want to improve the algorithm and remove more of the unused functions.  This especially means focusing on anonymous functions.

I also want to write JSlim plugins for Apache Ant and Apache Maven.  That way you can run JSlim as part of your build.  I'm still working on it.

I'd also like to thank the [Google Closure Compiler](http://code.google.com/closure/compiler/) for all of their help, support, encouragement, and excellent compiler.  JSlim stands on the shoulders of the Closure Compiler.

Feedback
--------------------------------------

I'm happy for any feedback you have on this project.  It is still in the early stages and I'm still working out the best heuristics.  JavaScript is a very weakly typed language and optimizing it is tricky.  

If you have time to try it out or any suggestions for how I can make JSlim better please send me an email.

License
--------------------------------------

This project is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
