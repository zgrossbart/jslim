(function() {
    var obj = {
        hello1: function() {
            alert('hello');
        },
        goodbye1: function() {
            alert('goodbye');
        }
    }

    obj['h' + 'el' + 'l' + 'o' + 1]();
})();
