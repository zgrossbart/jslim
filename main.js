obj1 = {
    func1: function() {
        alert("I'm func 1");
    },
    
    func2: function() {
        alert("I'm func 2");
    },
    
    func3: function() {
        alert("I'm func 3");
    }
}

function doSomething() {
    /*
     * I'm a block comment
     */
    var myVar = 'foo string';
    myVar = myVar.substring(1);
    
    var bar = 'bar string';
    alert('foo: ' + myVar);
    
    obj1.func1();
}

obj1.func3();
