obj1 = {
    func1: function() {
        return obj.func2;
    },

    func2: function() {
        return "I'm function 2";
    }
};

obj1.func1()();
