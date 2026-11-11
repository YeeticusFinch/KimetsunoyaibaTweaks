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
- Do some flash particles with some spark particles
- The lightning bolt's end position gets permanently set to the target location
- All valid target entities within 2 blocks of the trajectory (between the start location and target location) get damaged and get flash particles on them

Ticks 26-36
- User plays the unsheathing animation
- User is frozen

Tick 37
- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Sixfold

The user holds the pose a little longer, then dashes forward at great speed to wherever he was aiming, they will land completely still with the pose and must press (m1/m2) to immediately do it again, if there is a single target, it's recommended to aim, click, aim, click, aim, click, as fast as you can, mastering this means super quick attacks from many directions, but it is difficult to master the aim. (However if there are are targets all over the place, have fun happy-clicking)
For one, you might miss your target in one or two dashes. For two, if you take too long (about 2 seconds) aiming, the flow of the attack breaks and it simply stops, leaving you with the cooldown

The trajectory is marked by our custom lightning bolt, with the start position being the location where the form was activated, and the end position being the target location, and all the locations in-between being all the click target locations

Ticks 0-29
- Holding the unsheathing animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 30
- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- This lightning bolt has a lifetime of 40 ticks

Tick 30-70
- During this 40 tick period, every time the user right clicks or left clicks (either clicking an entity, the air, or a block), it does the launch sequence
- In this move, the user can do up to 6 launch sequences
- A launch sequence can't execute while one is already in progress, and each launch sequence lasts 5 ticks
- As soon as the 6th launch sequence has completed, it will break to the ending clause, ending the 40 ticks early
- Once these 40 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)
- Raycast 8 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast
- User plays the `sword_to_right` animation
Ticks 1-5 of launch sequence (do this every tick)
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 5 of the launch sequence
- The lightning bolt's end position gets updated every tick to be the user's location
Tick 5 of launch sequence (do this once per launch, at the end of the launch)
- Do some flash particles with some spark particles at the target location
- In the array of positions in the lightning bolt's path, set the last position to the target position
- And then append another position to that array, being the player's position, that new appended position will be the new end position, and this will be the end position that will be updated in the next launch
- All valid target entities within 2 blocks of this launch trajectory get damaged and get flash particles on them

Ending - 20 ticks
- User plays the unsheathing animation
- User is frozen

Cleanup - 20 ticks after ending
- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Eightfold

The user holds the pose yet a little longer, then dashes forward at great speed to where the crosshair was, the mechanic is the same as with sixfold, except of course, this one has two extra hits, longer initial pose and slightly longer cooldown.

The trajectory is marked by our custom lightning bolt, with the start position being the location where the form was activated, and the end position being the target location, and all the locations in-between being all the click target locations

Ticks 0-39
- Holding the unsheathing animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 40
- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- This lightning bolt has a lifetime of 40 ticks

Tick 40-100
- During this 60 tick period, every time the user right clicks or left clicks (either clicking an entity, the air, or a block), it does the launch sequence
- In this move, the user can do up to 8 launch sequences
- A launch sequence can't execute while one is already in progress, and each launch sequence lasts 3 ticks
- As soon as the 6th launch sequence has completed, it will break to the ending clause, ending the 60 ticks early
- Once these 60 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)
- Raycast 8 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast
- User plays the `sword_to_right` animation
Ticks 1-3 of launch sequence (do this every tick)
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 3 of the launch sequence
- The lightning bolt's end position gets updated every tick to be the user's location
Tick 3 of launch sequence (do this once per launch, at the end of the launch)
- Do some flash particles with some spark particles at the target location
- In the array of positions in the lightning bolt's path, set the last position to the target position
- And then append another position to that array, being the player's position, that new appended position will be the new end position, and this will be the end position that will be updated in the next launch
- All valid target entities within 2 blocks of this launch trajectory get damaged and get flash particles on them

Ending - 15 ticks
- User plays the unsheathing animation
- User is frozen

Cleanup - 15 ticks after ending
- Cleanup, user is unfrozen, animation stops
