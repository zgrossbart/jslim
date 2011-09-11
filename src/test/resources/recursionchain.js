obj1 = { };

obj1.func1 = function() {
    alert("I'm func1");
};

obj1.func2 = obj1.func3 = function() {
    for(var i = 0; i < 2; i++) {
        obj1.func2;
    }
};

obj1.func1();
