obj1 = { };

obj1.func1 = function() {
    $('#output').append("<br>I'm func 1");
};

obj1.func2 = function() {
    return "I'm func 2";
};

var s = obj1.func2();
alert('s: ' + s);
