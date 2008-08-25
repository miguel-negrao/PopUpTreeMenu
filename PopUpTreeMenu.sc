//redFrik
PopUpTreeMenu : SCViewHolder {
	var	<>tree, <value, <currentLeaf, <>action, <>openAction, <>closeAction,
		<font, <bounds, <hiliteColor, <parentWindow,
		pop, usr, hgt, lst,
		lastSelected= 0, xIndexLast, yIndexLast, mouseMoved;
	*new {|parent, bounds|
		^super.new.init(parent, bounds);
	}
	init {|parent, bounds|
		var cnt= 0;								//safety
		while({parentWindow.isNil or:{cnt>99}}, {
			cnt= cnt+1;
			if(parent.isKindOf(GUI.window), {
				parentWindow= parent;
			}, {
				if(parent.respondsTo(\findWindow), {
					parentWindow= parent.findWindow;
				}, {
					if(parent.isKindOf(GUI.compositeView) or:{parent.isKindOf(FlowView)}, {
						parent= parent.parent;
					});
				});
			});
		});
		font= GUI.font.new("Monaco", 9);
		hiliteColor= Color.black;
		pop= GUI.listView.new(parent, bounds)
			.font_(font)
			.background_(Color.clear)
			.hiliteColor_(hiliteColor)
			.stringColor_(Color.black);
		usr= GUI.userView.new(parent, bounds).relativeOrigin_(true);
		usr.mouseDownAction_({|v, x, y| mouseMoved= false; this.prUserAction(v, x, y)});
		usr.mouseMoveAction_({|v, x, y| mouseMoved= true; this.prUserAction(v, x, y)});
		usr.mouseUpAction_({|v, x, y| mouseMoved= false; this.prUserActionEnd(v, x, y)});
		lst= List.new;
		tree= (\nil: ());							//default tree
		pop.onClose= {lst.do{|z| if(z[1].notNil, {z[1].close})}};
		//here set perhaps add on window endFrontAction?
		this.view_(pop);
	}
	currentPath {
		^lst.collect{|z| z[3][z[2].value]};
	}
	
	//--overrides
	bounds_ {|argRect| bounds= argRect; pop.bounds_(bounds); usr.bounds_(bounds)}
	font_ {|argFont| font= argFont; pop.font_(font)}
	hiliteColor_ {|argColor| hiliteColor= argColor; pop.hiliteColor_(hiliteColor)}
	
	//--private
	prUserAction {|v, x, y|
		var xIndex, yIndex;
		if(lst.size==0, {							//check if at root level
			openAction.value(this, x, y);
			this.prSubmenu(v.bounds, nil, pop, nil);	//open a submenu
			xIndex= 0;							//force y update below
			xIndexLast= 0;
		}, {										//at some sub level
			xIndex= lst.detectIndex{|z| z[0].containsPoint(x@y)};
			if(xIndex.notNil, {
				if(xIndex!=xIndexLast, {
					if(xIndex>xIndexLast, {			//open a submenu if moving right
						this.prSubmenu(*lst[xIndex]);
					}, {							//else close submenus if open
						lst.copyRange(xIndex+2, lst.size-1).do{|z| z[1].close};
						lst= lst.copyRange(0, xIndex+1);
						yIndexLast= nil;
					});
					xIndexLast= xIndex;
				});
			});
		});
		if(xIndex.notNil, {
			yIndex= (y-lst[xIndex][0].top).div(hgt).min(lst[xIndex][2].items.size-1);
			if(yIndex!=yIndexLast, {
				if(lst.size-1>xIndex, {				//close a submenu if open
					lst.last[1].close;
					lst.pop;
				});
				lst[xIndex][2].value_(yIndex);
				this.prSubmenu(*lst[xIndex]);
				yIndexLast= yIndex;
				if(lst.size==2, {
					lastSelected= lst[1][2].value;	//remember submenu level1 state
				});
			});
		}, {
			yIndexLast= nil;
		});
	}
	prUserActionEnd {|v, x, y|
		var xIndex= lst.detectIndex{|z| z[0].containsPoint(x@y)};
		if(xIndex.isNil, {							//mouse released outside menu tree
			//nil.postln;
		}, {										//mouse released on node
			if(mouseMoved.not and:{xIndex==0}, {
				//'did not move'.postln;			//todo: for noclickmode later
			});
			if(currentLeaf.size>0, {
				value= currentLeaf;
				action.value(this, value);			//call action function
				pop.items_([value.last.asString]);
				pop.value= 0;
			}, {									//mouse released on node with submenu
				pop.items_([]);
			});
		});
		lst.do{|z| if(z[1].notNil, {z[1].close})};	//close all windows
		lst= List.new;
		xIndexLast= nil;
		yIndexLast= nil;
		closeAction.value(this, x, y);
	}
	prSubmenu {|bounds, window, listView, keys|
		var addy, subdict, items, newWidth, screenBounds;
		addy= lst.collect{|z| z[3][z[2].value]};		//collect keys
		if(addy.size==0, {							//check if at root level
			subdict= tree;
		}, {										//at some sub level
			subdict= this.prLookup(tree, addy);
		});
		if(subdict.size>0, {						//node not a leaf - create submenu
			keys= subdict.keys.asArray.sort;
			items= keys.collect{|z|					//assume only symbol keys in dict
				if(subdict[z].size>0, {
					z= (z++" >").asSymbol;			//add arrow to nodes with subnodes
				});
				z;
			};
			hgt= "".bounds(font).height+3;
			if(addy.size==0, {
				newWidth= bounds.width;				//force root level width to listview
			}, {
				newWidth= items.maxValue{|z| z.asString.bounds(font).width}.max(30)+14;
				bounds= bounds.moveBy(bounds.width, listView.value*hgt)
			});
			bounds= bounds.resizeTo(newWidth, keys.size*hgt);
			screenBounds= this.prToScreen(bounds);
			if(screenBounds.top<0, {				//check if submenu below screen bottom
				bounds= bounds.moveBy(0, screenBounds.top);
				screenBounds= this.prToScreen(bounds);
			});
			window= GUI.window.new("", screenBounds, false, false).front;
			listView= GUI.listView.new(window, Rect(0, 0, bounds.width, bounds.height))
				.font_(pop.font)
				.background_(pop.background)
				.hiliteColor_(hiliteColor)
				.stringColor_(pop.stringColor)
				.items_(items);
			//here later somehow test if in noclickmode and then track mouseposition from win
			//window.acceptsMouseOver= true;
			//listView.mouseOverAction_({|v, x, y| [v, x, y].postln});
			lst.add([bounds, window, listView, keys]);
			if(lst.size==2, {						//recall submenu level1 state
				listView.value= lastSelected;
			});
			currentLeaf= nil;
		}, {
			currentLeaf= addy;
		});
	}
	prToScreen {|bounds|
		^bounds.moveTo(
			parentWindow.bounds.left+bounds.left,
			parentWindow.bounds.height+parentWindow.bounds.top-bounds.top-bounds.height
		)
	}
	prLookup {|tree, addy|
		^if(addy.size>1, {
			this.prLookup(tree[addy[0]], addy.drop(1));
		}, {
			tree[addy[0].asSymbol];					//assume only symbol keys in dict
		});
	}
}