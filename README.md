JSlim - It's your favorite JavaScript library, only faster
==================================================

[JSlim Overview](/zgrossbart/jslim/raw/master/jslim.png)

JSlim is a JavaScript optimizer based on the [Google Closure Compiler](http://code.google.com/closure/compiler/), but instead of optimizing your code it removes unused code.

Most websites use JavaScript libraries like [JQuery](http://jquery.com/) or [Prototype](http://www.prototypejs.org/), but they don't use the whole library.  JSlim builds you a new version of your favorite JavaScript library with only the code you're using.

Building JSlim
--------------------------------------

This project builds with [Gradle](http://www.gradle.org).  Build the application by running gradle in the project root directory.

This project builds and runs on Windows, Mac, and Linux.

Using JSlim
--------------------------------------

<pre><code>
java JSlimRunner [options...] arguments...

 --charset VAL                          : Input and output charset for all files
                                          . By default, we accept UTF-8 as input
                                           and output US_ASCII
 --compilation_level [WHITESPACE_ONLY | : Specifies the compilation level to use
  SIMPLE_OPTIMIZATIONS | ADVANCED_OPTIM : . Options: WHITESPACE_ONLY, SIMPLE_OPT
 IZATIONS | NONE]                       : IMIZATIONS, ADVANCED_OPTIMIZATIONS
 --externs VAL                          : The file containing javascript externs
                                          . You may specify multiple
 --flagfile VAL                         : A file containing additional command-l
                                          ine options.
 --formatting [PRETTY_PRINT | PRINT_INP : Specifies which formatting options, if
 UT_DELIMITER]                          :  any, should be applied to the output
                                          JS. Options: PRETTY_PRINT, PRINT_INPUT
                                          _DELIMITER
 --help                                 : Displays this message
 --js VAL                               : The javascript filename. You may speci
                                          fy multiple
 --js_output_file VAL                   : Primary output filename. If not specif
                                          ied, output is written to stdout
 --lib_js VAL                           : The javascript library filename. You m
                                          ay specify multiple
 --logging_level [ALL | CONFIG | FINE | : The logging level (standard java.util.
  FINER | FINEST | INFO | OFF | SEVERE  : logging.Level values) for Compiler pro
 | WARNING]                             : gress. Does not control errors or warn
                                          ings for the JavaScript code under com
                                          pilation
 --no_validate                          : Pass this argument to skip the pre-par
                                          se file validation step.  This is fast
                                          er, but won't provide good error messa
                                          ges if the input files are invalid Jav
                                          aScript.
 --print_tree                           : Prints out the parse tree and exits
 --separate_files                       : Pass this argument to separate library
                                           files and the regular files into diff
                                          erent output files.  By default they a
                                          re combined into a single file.
 --skip_gzip                            : Skip GZIPing the results

</code></pre>

This repository includes a number of sample application you can use to try JSlim with.  The easiest place to start is with a simple JQuery/underscore.js.  You can slim it like this:

<pre><code>build/install/jslim/bin/jslim --js_output_file out.js --js main.js --lib_js libs/jquery-1.6.4.js --lib_js libs/underscore.js
</code></pre>
        
Once that command is done you can open the index.html file in your favorite browser and see this output:

<pre><code>2,4,6,8
Miaow my name is Charlie
Miaow my name is Fluffy
Miaow my name is Mouse
</code></pre>

This process will remove 160 out of 397 named functions (39.5 percent) and reduce the total size of the library files by 28 percent.

How JSlim works
--------------------------------------

JavaScript organizes blocks of code into functions.  Some functions are named and stand alone like this:

<pre><code>function myFunction() {
    alert('hey there');
}
</code></pre>

Other functions are organized into JavaScript closures like this:

<pre><code>var myObj = {
    myFunction: function() {
        alert('hey there');
    }
};
</code></pre>

JavaScript libraries provide functions like that and you call them in your code.  JSlim figures out which functions you're calling, and which ones they're calling, and removes the rest.  

JSlim can track most function calls, but there are some where it can't follow what you're calling.  The easiest way to break JSlim is using the [eval](http://en.wikipedia.org/wiki/Eval#JavaScript) function like this:

<pre><code>function myFunction() {
    alert('hey there');
}

eval('myfunction();');
</code></pre>

JSlim can't follow this function call to `myFunction` since it is part of an evaluated string.  In this case you must reference `myFunction` as an external reference.  Many JavaScript library do dynamic loading like this, especially for effects.

Can I see a simple example?
--------------------------------------

Let's say you write some JavaScript which looks like this:

<pre><code>o = {
    f1: function() {
        alert("I'm func 1");
    },
    
    f2: function() {
        alert("I'm func 2");
    }
};

o.f1();
</code></pre>

JSlim looks at this code and determines that <code>f1</code> is called, but <code>f2</code> isn't.  In this case it will remove <code>f2</code> and make the code smaller and faster.  If you start using <code>f2</code> just run JSlim again and it will create a new library with it included.

How much does JSlim save?
--------------------------------------

JSlim works very differently depending on the library it's slimming down.  The more a library uses named functions with simple dependencies and clean hierarchy the more we can slim it down.  

JSlim doesn't work well with libraries using mostly anonymous functions and other constructs which are difficult to track down.  JSlim is also strictly a [static code analysis](http://en.wikipedia.org/wiki/Static_code_analysis) tool which means it doesn't run any JavaScript code.  

For example, if your code looks like this:

<pre><code>function f(flag) {
    if (flag === 1) {
        alert($.now());
    } else if (flag === 2) {
        $.noop();
    }
}
</code></pre>

...JSlim will keep the `now` function and the `noop` function since it can't tell what the value of flag might be.

JSlim is best with a simple JQuery/underscore.js application where it reduces the total size of the libraries by 28% above using the Closure compiler and GZIPing.  Most libraries are reduced by 10-20 percent, but the savings in total size is only part of the story.

Most JavaScript compressors make your code smaller by removing whitespace and comments and inlining functions.  JSlim actually removes code which reduces [computational complexity](http://en.wikipedia.org/wiki/Computational_complexity_theory).  The means your libraries aren't just smaller, but they actually run faster.

How stable is JSlim?
--------------------------------------

JSlim is at release 0.1.  It's pretty stable, but hasn't had a large amount of testing yet.  I've tested JSlim with the following libraries:

* [Backbone.js](http://documentcloud.github.com/backbone/)
* [JQuery](http://jquery.com/)
* [JQuery UI](http://jqueryui.com/)
* [Modernizr](http://www.modernizr.com/)
* [Mootools](http://mootools.net/)
* [Prototype](http://www.prototypejs.org/)
* [Raphaël](http://raphaeljs.com/)
* [Underscore.js](http://documentcloud.github.com/underscore/)

JSlim should work with any library.  It can also work directly with your JavaScript.  Write as many functions as you want and JSlim will prune out the unused ones so your code stays small and your site stays fast.
  
JSlim roadmap
--------------------------------------

In the short term I'm focusing on improvements to the core compilation process.  I want to improve the algorithm and remove more of the unused functions.  This especially means focusing on anonymous functions.

I also want to write JSlim plugins for Apache Ant and Apache Maven.  That way you can run JSlim as part of your build.  I'm still working on it.

I'd also like to thank the [Google Closure Compiler](http://code.google.com/closure/compiler/) team for all of their help, support, encouragement, and excellent compiler.  JSlim stands on the shoulders of the Closure Compiler.

Why does JSlim build with Gradle?
--------------------------------------

JSlim could build with [Ant](http://ant.apache.org) or [Maven](http://maven.apache.org), but I wanted a new project to try out [Gradle](http://www.gradle.org).  I stuck with Gradle because it's awesome.  Check out my gradle.build file and see how easy it is to make a complex build in Gradle.

Feedback
--------------------------------------

I'm happy for any feedback you have on this project.  It is still in the early stages and I'm still working out the best heuristics.  JavaScript is a very weakly typed language and optimizing it is tricky.  

If you have time to try it out or any suggestions for how I can make JSlim better please send me an email.

License
--------------------------------------

This project is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
