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

var f = 'func4';

obj1[f] = function() {
    return "I'm function 4";
}

alert(obj1.func2());
alert(obj1.func3());
alert(obj1.func4());
