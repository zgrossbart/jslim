function mainFunc() {
    alert("I'm the main function");
}

obj1 = {
    func1: function() {
        alert("I'm func 1 callback");
    },
    
    func2: function() {
        alert("I'm func 2");
    },
    
    func3: function() {
        alert("I'm func 3");
    },
    
    func4: function() {
        alert("I'm func 4");
    },
    
    getObj: function() {
        return this;
    },
    
    init: function() {
        (function(window) {
            o.func4();
        }(window));
    }
}
