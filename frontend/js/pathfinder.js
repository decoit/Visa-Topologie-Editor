/*
 *  Copyright (C) 2013, DECOIT GmbH
 *
 *	This file is part of VISA Topology-Editor.
 *
 *	VISA Topology-Editor is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by the
 *	Free Software Foundation, either version 3 of the License, or (at your option)
 *	any later version.
 *
 *	VISA Topology-Editor is distributed in the hope that it will be useful, but
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *	more details.
 *
 *	You should have received a copy of the GNU General Public License along with
 *	VISA Topology-Editor. If not, see <http://www.gnu.org/licenses/>.
 */
 
// Create PathFinding namespace
var PathFinder = {};

PathFinder.line = {};

// Implementation of the Mikami-Tabuchi line search algorithm
// Uses a TABLE as grid and TD elements as nodes
// Title attributes of the TD elements are altered during path search
PathFinder.line.MikamiTabuchi = new function() {
	/* Enable strict mode */
	"use strict";

	// Attribute declaration
	var gridObj,                        // DOM object of the grid (TABLE element)
		pathCorners = [],				// Array holding the corners of the path
		rowNum, colNum,                 // Count of rows (TR) and cols (TD) in the grid
		iterationLevel,                 // Current level of line drawing iteration
		source, target,                 // Source and target node
		nodeList = {},					// Is used to store all nodes which will be checked in the next iteration
		lines = [],						// All drawn lines will be stored here, line identifier will be used as key, origin node as value
		lineCounter = [],				// Counting the drawn lines, separated in s (source) and t (target)
		touchedCells = [],				// All cells with added data attributes will be stored here
		intersectLine,                  // The line identifier of the line creating the intersection is stored here

	// Function declaration
		init,                           // Initialize needed variables for the path finder
		reset,
		findPath,                       // Do the path finding from source to target node
		doDrawLine,                     // Call drawLine for all four directions on a specific node
		drawLine;                       // // Draw a line in a specific direction, recursive method until an obstacle or grid bounds are hit

	// Initialize needed variables for the path finder
	// grid: DOM object of the grid (DOM object, TABLE element)
	// return: false, if grid was no TABLE element
	init = function(grid) {
		if(grid.nodeName === "TABLE") {
			// Store the grid and count rows and cols
			gridObj = grid;
			rowNum = gridObj.rows.length;
			colNum = gridObj.rows[0].cells.length;

			// Initialize node lists
			nodeList.s = [];
			nodeList.t = [];
		}
		else {
			// No TABLE element provided
			return false;
		}
	};

	// Reset the routing engine for the next search
	reset = function() {
		PathFinder.line.MikamiTabuchi.pathCorners = [];
	};

	// Do the path finding from source to target node
	// s: Source node (DOM object, TD element)
	// t: Target node (DOM object, TD element)
	findPath = function(s, t) {
		var nextNode = null,        // The next node which will be checked during the iteration
			intersection = false,   // The found intersection will be stored here, it is false while no intersection is found
			origin,                 // Used to track the path back to the source/target node
			cell,                   // Used to iterate through all touched cells and remove the 'data-mikami-tabuchi' attribute
			success;				// Flag: true, if path was found, false otherwise

		source = s;
		target = t;

		// Start at iteration level 0
		iterationLevel = 0;

		// Add the source and target cells to the node list for this iteration
		nodeList.s[iterationLevel] = [s];
		nodeList.t[iterationLevel] = [t];

		lineCounter.s = 0;
		lineCounter.t = 0;

		// Do iterations until we found an intersection (path found) or the node lists for the next iteration level are both empty (no path found)
		while((nodeList.s[iterationLevel].length > 0 || nodeList.t[iterationLevel].length > 0) && intersection === false) {
			// Create Arrays for the next iteration level (needed in drawLine())
			nodeList.s[iterationLevel+1] = [];
			nodeList.t[iterationLevel+1] = [];

			// Starting at source cell/cells connected to source cell
			// Get the first node for this iteration level
			nextNode = nodeList.s[iterationLevel].pop();

			// Iterate through the whole node list until an intersection was found or the list is empty
			while(nextNode !== undefined && intersection === false) {
				intersection = doDrawLine("s", nextNode);

				// Get the next node for this iteration level
				nextNode = nodeList.s[iterationLevel].pop();
			}

			// Now start at target cell/cells connected to target cell
			// Get the first node for this iteration level
			nextNode = nodeList.t[iterationLevel].pop();

			// Iterate through the whole node list until an intersection was found or the list is empty
			while(nextNode !== undefined && intersection === false) {
				intersection = doDrawLine("t", nextNode);

				nextNode = nodeList.t[iterationLevel].pop();
			}

			// Increase iteration level
			iterationLevel++;
		}

		// Did we find an intersection of source and target path?
		if(intersection !== false) {
			PathFinder.line.MikamiTabuchi.pathCorners.push(intersection);

			// Track back the path to source and target nodes
			if(intersection.getAttribute("data-mikami-tabuchi") !== null && intersection.getAttribute("data-mikami-tabuchi").indexOf("s") !== -1) {
				// Hit line came from source node
				// Track back to source node
				origin = lines[intersection.getAttribute("data-mikami-tabuchi")];
				while(origin !== source) {
					PathFinder.line.MikamiTabuchi.pathCorners.unshift(origin);
					origin = lines[origin.getAttribute("data-mikami-tabuchi")];
				}

				// Track back to target node
				origin = lines[intersectLine];
				while(origin !== target) {
					PathFinder.line.MikamiTabuchi.pathCorners.push(origin);
					origin = lines[origin.getAttribute("data-mikami-tabuchi")];
				}
			}
			else {
				// Hit line came from target node
				// Track back to target node
				origin = lines[intersection.getAttribute("data-mikami-tabuchi")];
				while(origin !== target) {
					PathFinder.line.MikamiTabuchi.pathCorners.push(origin);
					origin = lines[origin.getAttribute("data-mikami-tabuchi")];
				}

				// Track back to source node
				origin = lines[intersectLine];
				while(origin !== source) {
					PathFinder.line.MikamiTabuchi.pathCorners.unshift(origin);
					origin = lines[origin.getAttribute("data-mikami-tabuchi")];
				}
			}

			PathFinder.line.MikamiTabuchi.pathCorners.unshift(source);
			PathFinder.line.MikamiTabuchi.pathCorners.push(target);

			success = true;
		}
		else {
			success = false;
		}

		// Remove 'data-mikami-tabuchi' attributes on all touched cells
		cell = touchedCells.pop();
		while(cell !== undefined) {
			cell.removeAttribute("data-mikami-tabuchi");
			cell = touchedCells.pop();
		}

		intersectLine = null;

		return success;
	};

	// Call drawLine for all four directions on a specific node
	// nodeLabel: Label of the origin node (String, "s": source, "t": target)
	// nextNode: Origin for the new lines (DOM object, TD element)
	doDrawLine = function(nodeLabel, nextNode) {
		var rv;     // The return value of the drawLine method will be stored here

		// Draw vertical line
		lines[nodeLabel + String(lineCounter[nodeLabel])] = nextNode;
		if(source.parentNode.rowIndex > target.parentNode.rowIndex) {
			rv = drawLine(nodeLabel, "up", nextNode.parentNode.rowIndex-1, nextNode.cellIndex);

			if(rv === false) {
				rv = drawLine(nodeLabel, "down", nextNode.parentNode.rowIndex+1, nextNode.cellIndex);
			}
		}
		else {
			rv = drawLine(nodeLabel, "down", nextNode.parentNode.rowIndex+1, nextNode.cellIndex);

			if(rv === false) {
				rv = drawLine(nodeLabel, "up", nextNode.parentNode.rowIndex-1, nextNode.cellIndex);
			}
		}
		lineCounter[nodeLabel]++;


		// Set origin of horizontal line
		lines[nodeLabel + String(lineCounter[nodeLabel])] = nextNode;
		// Draw horizontal line
		if(source.cellIndex > target.cellIndex) {
			if(rv === false) {
				rv = drawLine(nodeLabel, "left", nextNode.parentNode.rowIndex, nextNode.cellIndex-1);
			}

			if(rv === false) {
				rv = drawLine(nodeLabel, "right", nextNode.parentNode.rowIndex, nextNode.cellIndex+1);
			}
		}
		else {
			if(rv === false) {
				rv = drawLine(nodeLabel, "right", nextNode.parentNode.rowIndex, nextNode.cellIndex+1);
			}

			if(rv === false) {
				rv = drawLine(nodeLabel, "left", nextNode.parentNode.rowIndex, nextNode.cellIndex-1);
			}
		}
		lineCounter[nodeLabel]++;

		return rv;
	};

	// Draw a line in a specific direction, recursive method until an obstacle or grid bounds are hit or an intersection of source and target is found
	// Returns a TD DOM object of the intersection if found, false in all other cases
	// nodeLabel: Label of the origin node (String, "s": source, "t": target)
	// direction: Direction of the new line (String, "up", "right", "down", "left")
	// row: Number of the current cell's row (integer)
	// col: Number of the current cell's col (integer)
	drawLine = function(nodeLabel, direction, row, col) {
		var r,      // Row number for the next recursion
			c,      // Col number for the next recursion
			prevr,
			prevc,
			cableID,
			cableIDRegEx;

		// Calculate the row and col for the next recursion
		switch(direction) {
			case "up":
				r = row-1;
				c = col;

				prevr = row+1;
				prevc = col;
				break;
			case "right":
				r = row;
				c = col+1;

				prevr = row;
				prevc = col-1;
				break;
			case "down":
				r = row+1;
				c = col;

				prevr = row-1;
				prevc = col;
				break;
			case "left":
				r = row;
				c = col-1;

				prevr = row;
				prevc = col+1;
				break;
			default:
				return false;
		}

		// Check, if the cell for this recursion lies outside the grid bounds or has the class "obstacle" on it
		if(row < 0 || row === rowNum || col < 0 || col === colNum || gridObj.rows[row].cells[col].className.indexOf("obstacle") > -1) {
			// Cell is invalid, return false
			return false;
		}
		else {
			// Prevent parallel cables on the same row or col
			if(gridObj.rows[row].cells[col].className.search(/\bcable\b/) > -1) {
				gridObj.rows[row].cells[col].className.match(/\b(ncable(_virtual)?_\d+)\b/);
				cableID = RegExp.$1;

				cableIDRegEx = new RegExp("\\b" + cableID + "\\b");

				if(gridObj.rows[r] !== undefined && gridObj.rows[r].cells[c] !== undefined &&
				   gridObj.rows[r].cells[c].className.search(/\bcable\b/) > -1 &&
				   gridObj.rows[r].cells[c].className.search(cableIDRegEx) > -1) {
					return false;
				}
				else if(gridObj.rows[prevr] !== undefined && gridObj.rows[prevr].cells[prevc] !== undefined &&
						gridObj.rows[prevr].cells[prevc].className.search(/\bcable\b/) > -1 &&
						gridObj.rows[prevr].cells[prevc].className.search(cableIDRegEx) > -1) {
					return false;
				}
			}

			// Check, if the 'data-mikami-tabuchi' attribute of the current cell matches our scheme of marks
			if(gridObj.rows[row].cells[col].getAttribute("data-mikami-tabuchi") !== null && gridObj.rows[row].cells[col].getAttribute("data-mikami-tabuchi").search(/^([st]{1})(\d+)$/) > -1) {
				// If the node label of this cell does not match the node label of this recursion, we found an intersection (HOORAY!)
				if(RegExp.$1 !== nodeLabel) {
					intersectLine = nodeLabel + String(lineCounter[nodeLabel]);
					return gridObj.rows[row].cells[col];
				}
				else {
					// If it has the same node label, return false since we're done with this line
					return false;
				}
			}
			else {
				// Is this node identical to source or target cell? If not, apply operations
				if(gridObj.rows[row].cells[col] !== source && gridObj.rows[row].cells[col] !== target) {
					// We found a cell that was not touched before, give it our mark (nodeLabel_iteration_segment)
					gridObj.rows[row].cells[col].setAttribute("data-mikami-tabuchi", nodeLabel + String(lineCounter[nodeLabel]));
					touchedCells.push(gridObj.rows[row].cells[col]);

					// Add it to the list of nodes for the next iteration
					nodeList[nodeLabel][iterationLevel+1].unshift(gridObj.rows[row].cells[col]);
				}
			}

			// Start next recursion
			return drawLine(nodeLabel, direction, r, c);
		}
	};

	return {
		// Public functions
		init : init,
		reset : reset,
		findPath : findPath,

		// Public attributes
		pathCorners : pathCorners
	};
};
