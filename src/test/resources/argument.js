obj1 = { };

obj1.func1 = function() {
    $('#output').append("<br>I'm func 1");
};

obj1.func2 = function() {
    return "I'm func 2";
};

alert('obj1.func2(): ' + obj1.func2());
