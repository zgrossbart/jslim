obj1 = {
    func1: function(msg) {
        alert('msg: ' + msg);
    }
};

obj1.func1.call(window, 'testing');
