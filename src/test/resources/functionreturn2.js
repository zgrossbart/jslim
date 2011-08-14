obj1 = {
    func1: function() {
        if (this.func2) return this.func2;
        var _func2 = this;
        
        return this.func2 = function() {
            var a = update([this], arguments);
            return _func2.apply(null, a);
        };
    }
};

obj1.func1()();
