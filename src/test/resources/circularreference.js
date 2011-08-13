obj1 = { };

obj1.func1 = function() {
    obj1.func3();
};

obj1.func2 = function() {
    return "I'm func 2";
};

obj1.func3 = function() {
    obj1.func1();
};

alert('obj1.func2(): ' + obj1.func2());
