

var domInterop = {};

domInterop.getNodeIndexesToNode = function getNodeIndexesToNode(node) {
	if (node.parentNode!==null) {
		var lst = domInterop.getNodeIndexesToNode(node.parentNode);
		var index = null;
		for (var i=0; i<node.parentNode.childNodes.length; i++) {
			if (node.parentNode.childNodes.item(i)===node) {
				index = i;
				break;
			}
		}
		console.log(node.parentNode, "["+index+"]", node);
		lst.push(index);
		return lst;
	} else {
		return [];
	}
};

