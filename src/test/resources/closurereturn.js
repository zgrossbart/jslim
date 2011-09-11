obj1 = {
    func1: function() {
        return {
            func1_1: function() {
                alert("I'm function 1_1");
            },

            func1_2: function() {
                alert("I'm function 1_2");
            },
            
            func1_2: function() {
                alert("I'm function 1_2");
            }
        };
    }
};

obj1.func1().func1_1();
