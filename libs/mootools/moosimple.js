window.addEvent('domready', function() {
   console.log('domready...');

   var element = $('myElement');
   
   console.log('element: ' + element);
   
   $("myElement").setStyles({
       background: "gold",
       border:"solid 1px #999999",
       margin: "75px auto",
       width: "700px",
       padding: "3em",
       height: "80px"
   });
});
