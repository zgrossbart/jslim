obj1 = {
    func1: function() {
        return "I'm function 1";
    },

    func2: function() {
        return "I'm function 2";
    }
};

var ar = [];
ar['func1'] = obj1.func1();
alert("ar['func1']: " + ar['func1']);
