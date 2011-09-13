obj1 = {
    func1: function() {
        return "I'm function 1";
    }
};

obj1['func2'] = function() {
    return "I'm function 2";
};

obj1['fu' + 'nc' + 3] = function() {
    return "I'm function 3";
}

alert(obj1.func2());
alert(obj1.func3());
