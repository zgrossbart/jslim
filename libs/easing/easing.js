window.onload = function () {
    var r = Raphael("holder", 800, 600);
    var targets = r.set();
    targets.push(r.circle(300, 100, 20),
       r.circle(300, 150, 20),
       r.circle(300, 200, 20),
       r.circle(300, 250, 20),
       r.circle(300, 300, 20),
       r.circle(300, 350, 20),
       r.circle(300, 400, 20),
       r.circle(300, 450, 20));
    targets.attr({fill: "#000", stroke: "#fff", "stroke-dasharray": "- ", opacity: .2});
    var labels = r.set();
    labels.push(r.text(330, 100, "linear (default)"),
                r.text(330, 150, ">"),
                r.text(330, 200, "<"),
                r.text(330, 250, "<>"),
                r.text(330, 300, "bounce"),
                r.text(330, 350, "elastic"),
                r.text(330, 400, "backIn"),
                r.text(330, 450, "backOut"));
    labels.attr({font: "12px Fontin-Sans, Arial", fill: "#fff", "text-anchor": "start"});
    var movers = r.set();
    movers.push(r.circle(100, 100, 20),
                r.circle(100, 150, 20),
                r.circle(100, 200, 20),
                r.circle(100, 250, 20),
                r.circle(100, 300, 20),
                r.circle(100, 350, 20),
                r.circle(100, 400, 20),
                r.circle(100, 450, 20));
    movers.attr({fill: "#000", stroke: "#fff", "fill-opacity": 0});
    movers[0].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(0, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000);
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[1].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.1, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, ">");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[2].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.2, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "<");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[3].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.3, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "<>");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[4].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.4, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "bounce");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[5].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.5, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "elastic");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[6].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.6, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "backIn");
        this.cx = this.cx == 300 ? 100 : 300;
    });
    movers[7].click(function () {
        this.cx = this.cx || 300;
        this.animate({cx: this.cx, "stroke-width": this.cx / 100, fill: this.cx - 100 ? "hsb(.7, .75, .75)" : "#000", "fill-opacity": +!!(this.cx - 100)}, 1000, "backOut");
        this.cx = this.cx == 300 ? 100 : 300;
    });
};
