obj1 = {
    func1: function() {
        alert("I'm func 1");
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
    
    init: function() {
        (function(window) {
            obj1.func4();
        }(window));
    }
}
