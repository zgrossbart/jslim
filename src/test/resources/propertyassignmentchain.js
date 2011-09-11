obj1 = { };

obj1.func1 = obj1.func2 = obj1.func3 = function() {
        return "I'm function 1, 2, and 3";
};

obj1.func4 = obj1.func5 = obj1.func6 = function() {
        return "I'm function 4, 5, and 6";
};

obj1.func7 = function() {
        return obj1.func6();
};

obj1.func1();
