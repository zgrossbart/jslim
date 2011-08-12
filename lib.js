function mainFunc() {
    alert("I'm the main function");
}

obj1 = {
    func1: function() {
        $('#output').append("<br>I'm func 1");
    },
    
    func2: function() {
        //$('#output').append("<br>I'm func 2");
        obj1.func3();
    },
    
    func3: function() {
        $('#output').append("<br>I'm func 3");
    }/*,
    
    func4: function() {
        $('#output').append("<br>func4 is about to call AJAX");
        
        $.ajax({
            url: "test.html",
            success: function() {
                $('#output').append('<br>Success');
            },
            error: function(xhr, ajaxOptions, thrownError){
                 $('#output').append('<br>AJAX Error ' + xhr.status + ':');
                 $('#output').append('<br>' + thrownError);
            }
        });
        
        obj1.func3();
    },
    
    getObj: function() {
        return this;
    },
    
    init: function() {
        (function(window) {
            obj1.func4();
        }(window));
    }*/
};

/*obj2 = {
    func2_1: function() {
        $('#output').append('<br>This is function 1 from object 2');
    }
};

jQuery(document).ready(function() {
    obj2.func2_1();
});*/
