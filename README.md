Prune - It's your favorite JavaScript library, only faster
==================================================

This is a project for doing something really cool with JavaScript which doesn't work yet.  Right now this project is just for people actively developing this code and it won't even build for you.  

The project depends on the [Google Closure Compiler](http://code.google.com/closure/compiler/) and it can't run with the version from Maven central.  You must download the source for this compiler and reference the built compiler.jar file in the build.gradle file.  I'm working on making this easier.

This project builds with [Gradle](http://www.gradle.org).  Build the application by running gradle in the root directory.

Once you've built the project you can use it to do what it does (notice how I didn't say what that is yet).  There are many test scripts checked in, but this is a popular choice:

        build/install/jslim/bin/jslim --js_output_file out.js --js main.js --lib_js libs/jquery-1.6.2.js --lib_js libs/underscore.js

We're working on it :).

License
--------------------------------------

This project is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
