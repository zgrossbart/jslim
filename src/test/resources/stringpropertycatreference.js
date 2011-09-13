(function() {
    var obj = {
        hello: function() {
            alert('hello');
        },
        goodbye: function() {
            alert('goodbye');
        }
    }

    obj['h' + 'el' + 'l' + 'o']();
})();
