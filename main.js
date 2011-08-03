function CatMaker(name) {
    return {
        speak: function () {
            $('#output').append("<br>Miaow my name is " + name);
        }
    };
}

jQuery(document).ready(function() {
    //$('#output').append('It works!');
    
    //$('#dialog').dialog();
    
    //obj1.init();
    
    //$('#output').append('_isDate: ' + _.isDate('foo'));
    
    var arr = [1, 2, 3, 4];
    var newArr = _.map(arr, function (item) {
        return item * 2;
    });
    
    $('#output').append("<br>" + newArr);
    
    var catNames = ['Charlie', 'Fluffy', 'Mouse'];
    var cats = _.map(catNames, function (name) {
        return CatMaker(name);
    });
    
    _.each(cats, function (cat) {
        cat.speak();
    });
});
