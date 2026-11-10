**Colors**
The normal thunder breathing colors are yellow and light blue
The blue variant of thunder breathing (for the blue thunder breathing swords) are dark blue and light blue
For Kaigaku's sword, the colors are dark blue and black

Normal Swords (demon slayers):
- Primary Color: Yellow
- Secondary Color: Light Blue

Blue Variant Swords (demon slayers):
- Primary Color: Dark Blue
- Secondary Color: Light Blue

Normal Swords (demons):
- Primary Color: Yellow
- Secondary Color: Black

Blue Variant Swords (demons) and Kaigaku's Sword:
- Primary Color: Dark Blue
- Secondary Color: Black

**Lightning Bolts**

Thunder breathing relies on lightning bolts for visual effects
These lightning bolts are similar to the vanilla ones, except they have a start position and an end position, and they have multiple colors
A lightning bolt is a square beam that is one block wide. It is drawn as multiple beams super imposed on each-other.
- The outer beam is a full block wide, and it has 40% opacity
- Then there is another beam that is 0.8 blocks wide with 60% opacity
- Then another beam that is 0.6 blocks wide with 80% opacity
- Then a final inner beam that is 0.5 blocks wide with 100% opacity (this one is always white)

All 3 outer beams (the 40% opacity, 60%, and 80%) all have the same color, they use a gradient color, shifting between primary color and secondary color, with primary color being at the start location and secondary color being at the end location
- For the normal swords (the ones that are yellow and light blue), it should shift to white in the middle of the gradient instead of green, so the gradient should go yellow -> white -> lightb-lue

The beams are glowing too.

When a lightning bolt spawns, it is given an array of positions that define its path, with the first item being the start position and the last item being the end position, and it's given a lifetime too (default lifetime 15 ticks)
- During its lifetime, more positions can be appended to the lightning bolt, and the existing positions can be modified

## First form: Thunderclap and Flash

The user holds an unsheathing pose then dashes forward at great speed, must aim properly, performing a single strong slash to any opponent in their path.
The attack goes right through them, they simply take the damage if they're in the way.
The trajectory is marked by our custom lightning bolt, with the start position being the location where the form was activated, and the end position being the target location.

Ticks 0-19
- Holding the unsheathing animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 20
- Raycast 10 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast

Tick 21
- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- User plays the `sword_to_right` animation

Ticks 21-25
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 25
- The lightning bolt's end position gets updated every tick to be the user's location

Tick 25
- The lightning bolt's end position gets permanently set to the target location
- All valid target entities within 2 blocks of the trajectory (between the start location and target location) get damaged

Ticks 26-36
- User plays the unsheathing animation
- User is frozen

Tick 37
- Cleanup, user is unfrozen, animation stops
