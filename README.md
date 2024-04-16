# Advanced Locate

## Features

This mod aims to upgrade the Vanilla locate command by adding new functionalities.
It adds multiple structure finder and a slime chunk finder.
Slime chunk locating system, which you can ise to locate the nearest slime chunk, or the point with the highest density of slime chunks around.

## Usage

* `/loc structure nearest (amount) [structure]`, `amount` is the number of structures you want to be displayed, max being 10 and set to 5 by default. `structure` is the structure you want to search for. Please note it may not be totally accurate due to the finding algorithm.

* `/slime nearest`, locates the nearest slime chunk.

* `/slime density [radius] [neighbour_radius]`, `radius` being the radius within you want to search for, and `neighbour_radius` being the radius for which the slime chunks are counted

