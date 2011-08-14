$(document).ready(function() {
    if (Modernizr.websockets) {
        $("#result").html('Your browser has support for Web Sockets');
    } else {
        $("#result").html('Your browser does not support Web Sockets');
    }
});
