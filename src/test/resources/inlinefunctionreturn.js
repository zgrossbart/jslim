obj1 = {
    func1: function() {
        function newf() {
            return "I'm newf";
        }
        return newf;
    },

    func2: function() {
        return "I'm function 2";
    }
};

obj1.func1()();
