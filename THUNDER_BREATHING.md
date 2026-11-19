**Colors**

The normal thunder breathing colors are yellow and light blue
The blue variant of thunder breathing (for the blue thunder breathing swords) are dark blue and light blue
For Kaigaku's sword (human), the colors are the same as the blue variant sword (dark blue and light blue)
For Kaigaku's sword (demon), the colors are dark blue and black

Normal Swords (demon slayers):

- Primary Color: Yellow
- Secondary Color: Light Blue

Blue Variant Swords (demon slayers) and Kaigaku's Sword (human):

- Primary Color: Dark Blue
- Secondary Color: Light Blue

Normal Swords (demons):

- Primary Color: Yellow
- Secondary Color: Black

Blue Variant Swords (demons) and Kaigaku's Sword (demon):

- Primary Color: Dark Blue
- Secondary Color: Black

**Swords**

Normal Sword
- Has forms 1-6

Blue Variant Sword:
- Has forms 1-6

Kaigaku's Sword (human)
- Has forms 2-6

Zenitsu's Sword
- Has form 1, and all the variations for form 1 (sixfold, eightfold, and godspeed)
- Has form 7

Kaigaku's Sword (demon)
- Has forms 2-6 (with the kaigaku demon modifications, blue and black colors, and electrocuted effect)
- Has the hashira sword damage instead of the regular sword damage

Hashira Sword
- Has forms 1-7
- Has the hashira sword damage instead of the regular sword damage

Zenitsu's Sword (Full Potential)
- Has forms 1-7, and all the variations for form 1 (sixfold, eightfold, and godspeed) as actual variations rather than independent forms (meaning you use the variation cycler key on form 1 to access the variations)
- Has the hashira sword damage instead of the regular sword damage


**Lightning Bolts**

Thunder breathing relies on lightning bolts for visual effects
These lightning bolts are similar to the vanilla ones, except they have a start position and an end position, and they have multiple colors
A lightning bolt is a square beam that is one block wide. It is drawn as multiple beams super imposed on each-other.

- The outer beam is a full block wide, and it has 20% opacity
- Then there is another beam that is 0.8 blocks wide with 50% opacity
- Then another beam that is 0.6 blocks wide with 75% opacity
- Then a final inner beam that is 0.5 blocks wide with 100% opacity (this one is always white)

All 3 outer beams (the 20% opacity, 50%, and 75%) all have the same color, they use a gradient color, shifting between primary color and secondary color, with primary color being at the start location and secondary color being at the end location

- For the normal swords (the ones that are yellow and light blue), it should shift to white in the middle of the gradient instead of green, so the gradient should go yellow -> white -> lightb-lue

The beams are glowing too.

When a lightning bolt spawns, it is given an array of positions that define its path, with the first item being the start position and the last item being the end position, and it's given a lifetime too (default lifetime 15 ticks)

- During its lifetime, more positions can be appended to the lightning bolt, and the existing positions can be modified

**Lightning Sparks**

Lightning Sparks are similar to lightning bolts. However they are simpler and usually smaller.

Lightning Sparks don't take an array of positions, they just take a start position and an end position as inputs, along with a width (in blocks). The width is a float, so it can be less than a block. The inputted width refers to the width of the outer beam, and then the inner beam is half that width. It also takes in two colors, an outer color (default light blue) and an inner color (default white). The Lightning Spark is also given a tick lifetime as an input. Also, the start position and end position of the lightning sparks should be changeable. And it should also be possible to give the Lightning Sparks a velocity vector (the velocity vector gets added to both the start position and the end position on every tick).

Lightning Sparks also take a flickering boolean. If flickering is true, then they only render every other tick, so visible for 1 tick, not visible for 1 tick, visible for 1 tick, not visible for 1 tick.

Lightning Sparks also takes in an optional arcing vector. The arcing vector (default 0 0 0) provides both a direction and magnitude to arc the spark segments.

Lightning sparks are just two beams, an outer beam and an inner beam of half size.

- The outer beam is 50% opacity
- The inner beam is 100% opacity
- Both beams are glowing

A Lightning Spark can have multiple segments depending on its length and width. If the length squared of the segment (length squared because it's distance between start position and end position, and square roots are expensive, so just multipy the coefficients by eachother to avoid doing any power functions), if the length squared of the segment is less than 25x the width, then there will only be one segment. If the length squared is 25x the width, then there will be two segments. So ergo the number of segments equals the length squared divided by 25 multiplied by the width (length squared is in numerator, the width multiplying 25 is in the denominator). There will be a minimum of 1 segment, and a maximum of 50 segments (let's guard for divide by 0 errors).

If there is a non-zero arcing vector, then each of the segments will arc in the direction of that arcing vector, depending on proximity to middle. So like the edges (start position and end position) aren't affected at all. The middle position is shifted by 100% of the arc vector, so for that position in the middle of the segments, the arc vector should be added directly to it. For the position at 25% through the segments, and 75% through the segments, half of the arc vector should be added.

The first segment starts at the start position of the Lightning Spark. The last segment ends at the end position of the Lightning Spark. The positions of all the other points for the segments are calculated every other tick (so half are calculated on one tick, the other half are calculated on the other tick). These intermediate point locations are just calculated locally, like in rendering, no need to sync this with server, as it's just graphics. They are calculated by arranging the points sequentially in a straight line between the start position and end position, and then adding a random offset to every point in a random direction, the offset magnitude being a maximum of 5\*width.

## Electrocuted Effect (Kaigaku Effect)

When the user is using Kaigaku's sword, any entities that take damage from one of the forms will get the Electrocuted Effect

Turns your hearts into the electrocuted hearts textures

Plays the kaigaku_effect sound on loop

## First form: Thunderclap and Flash

The user holds the iai1 animation (the iai1 animation is the unsheathing pose) then dashes forward at great speed, must aim properly, performing a single strong slash to any opponent in their path.
The attack goes right through them, they simply take the damage if they're in the way.
The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location.

Tick 0
- Play the thunder_chargeup sound

Ticks 0-19

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 20

- Raycast 20 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast

Tick 21

- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- User plays the `sword_to_right` animation
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ticks 21-25

- User is launched to the target location, their velocity fixed such that they reach the target location by tick 25
- The lightning bolt's end position gets updated every tick to be the user's location

Tick 25

- Do some flash particles with some spark particles
- The lightning bolt's end position gets permanently set to the target location
- All valid target entities within 2 blocks of the trajectory (between the start location and target location) get damaged and get flash particles on them
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Ticks 26-36

- User plays the unsheathing animation
- User is frozen

Tick 37

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Sixfold

The user holds the pose a little longer, then dashes forward at great speed to wherever he was aiming, they will land completely still with the pose and must press (m1/m2) to immediately do it again, if there is a single target, it's recommended to aim, click, aim, click, aim, click, as fast as you can, mastering this means super quick attacks from many directions, but it is difficult to master the aim. (However if there are are targets all over the place, have fun happy-clicking)
For one, you might miss your target in one or two dashes. For two, if you take too long (about 2 seconds) aiming, the flow of the attack breaks and it simply stops, leaving you with the cooldown

The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location, and all the locations in-between being all the click target locations

Tick 0
- Play the thunder_chargeup_sixfold sound

Ticks 0-29

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 30

- A lightning bolt is created, with the start point being the starting location, and the end point being the user's current location
- This lightning bolt has a lifetime of 40 ticks

Tick 30-70

- During this 40 tick period, every time the user right clicks or left clicks (either clicking an entity, the air, or a block), it does the launch sequence
- In this move, the user can do up to 6 launch sequences
- Each click launches up to 15 blocks, and a launch sequence can't execute while one is already in progress. Clicks during an active launch are queued and launch as soon as the current launch ends. Each launch sequence lasts 5 ticks
- As soon as the 6th launch sequence has completed, it will break to the ending clause, ending the 40 ticks early
- Once these 40 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)

- Raycast 15 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
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
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ending - 20 ticks

- User plays the unsheathing animation
- User is frozen
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Cleanup - 20 ticks after ending

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Eightfold

The user holds the pose yet a little longer, then dashes forward at great speed to where the crosshair was, the mechanic is the same as with sixfold, except of course, this one has two extra hits, longer initial pose and slightly longer cooldown.

The trajectory is marked by our custom lightning bolt, with the start position being 10 blocks behind the location where the form was activated (opposite the user's look direction), and the end position being the target location, and all the locations in-between being all the click target locations

Tick 0
- Play the thunder_chargeup_eightfold sound

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
- Each click launches up to 10 blocks, and a launch sequence can't execute while one is already in progress. Clicks during an active launch are queued and launch as soon as the current launch ends. Each launch sequence lasts 3 ticks
- As soon as the 8th launch sequence has completed, it will break to the ending clause, ending the 60 ticks early
- Once these 60 ticks have expired (going past tick 65), it will go to the ending clause

Launch:
first tick of launch sequence (do this once per launch)

- Raycast 10 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
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
- Play a loud thunderclap sound on the user, and for all players within 30 blocks

Ending - 15 ticks

- User plays the unsheathing animation
- User is frozen
- Create 4 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=true, and last 8 ticks lifetime duration. Their color is secondary color.

Cleanup - 15 ticks after ending

- Cleanup, user is unfrozen, animation stops

## First form (variation): Thunderclap and Flash, Godspeed

Longer pose, but thicker hitbox, greater distance, overall greater speed, nearly tp speed, but same damage, however it has the longest cooldown among these variations. (the sound of totem of undying plays)
Downside: When dashing you take some damage, at least 3 hearts due to the great force used on your legs. 

Tick 0
- Play the thunder_chargeup_godspeed sound
- Create 5 Lightning Sparks, with the start location being the center of the user (halfway between the user's position and head position, accounting for local gravity direction), the end location being a random solid block within 2 blocks (or if no solid blocks are near, then just a random point 2 blocks away and no arcing), and the arcing vector being [0, 1, 0]. They should use an outer width of 0.0875 blocks, flickering=false, and last 50 ticks lifetime duration, and the color should be primary color

Ticks 0-49

- Holding the iai1 animation
- Staying absolutely still, can't move around (but can still rotate)
- If the user isn't a player, then the user will be always looking directly at their target

Tick 50

- A lightning bolt is created, with the start point being 10 blocks behind the user's starting location, and the end point being the target location of the first launch
- User plays the `speed_attack_sword` animation
- Play a loud thunder_godspeed sound on the user, and for all players within 100 blocks

Ticks 50-80

Do a launch sequence every 3 ticks (the user doesn't need to continuously click to do them), it will automatically do 10 sequential launch sequences

Tick 80
- Do some flash particles with some spark particles at the final target location
- All entities within 12 blocks of the final target location take the damage and get a primary color lightning spark going from their center (halfway between their location and their head location) to 1 block above the target location of the form (local gravity aware)
- Create 5 lightning sparks at the target location, their start position being set to 1 block above the target location (local gravity aware), and their end position being set to a random location 12 blocks away, color being primary color
- Every entity that gets damaged will get a flash particle on them
- User takes 6 points of damage

Launch:
First tick of launch sequence (do this once per launch)

- Raycast 10 blocks forward from the user's head in the direction the user is looking, stopping the raycast only if a block is hit (the raycast goes through blocks but not entities)
- Save that target location at the end of the raycast
- User plays the `speed_attack_sword` animation
- Create 1 lightning spark, with the start point being the user's location location at the start of this launch sequence, and the end point following the user, the color being primary color, and the outer width being 0.5 blocks

Ticks 1-3 of launch sequence (do this every tick)
- User is launched to the target location, their velocity fixed such that they reach the target location by tick 3 of the launch sequence
- The lightning bolt's end position gets updated every tick to be the user's location
- The lightning spark's end position gets updated every tick to be the user's location

Tick 3 of launch sequence (do this once per launch, at the end of the launch)
- In the array of positions in the lightning bolt's path, set the last position to the target position
- And then append another position to that array, being the player's position, that new appended position will be the new end position, and this will be the end position that will be updated in the next launch
- All valid target entities within 6 blocks of this launch trajectory get damaged and get flash particles on them

Ticks 80-105

- User plays the iai1 animation
- User is frozen

Tick 106

- Cleanup, user is unfrozen, animation stops


## Second Form: Rice Spirit

The user dashes, seeminlgy going right through them, but in reality user delivers five very fast cuts, the opponent it's seemingly paralyzed after the user went past them, then they receive the five hit damage, the last one has great knockback, the visual is as if the opponent is electrified then receives a final shock that sends them flying. (Similar to love breathing first form except this only hits one target, has greater damage, paralysis, and extra knockback)

Tick 0:

- Set the user's step-up height to 3 blocks (must be cleared by the end of the form)
- Play the rice_spirit_dash sound effect on the user (and for all players within 20 blocks of the user)

Ticks 0-5:

- Play the sprinting animation as the user dashes forward
- The user dashes up to 20 blocks forward in these 5 ticks
- The closest valid target living entity within 10 blocks of the user's path gets marked (stored in a variable)
- The target entity gets paralyzed (the immovable potion effect with no particles for 30 ticks)

Ticks 6-30:

- User plays the iai1 animation
- User has 0 movement speed (can't move, but they can rotate)
- The target is surrounded by thunder particles from the kimetsunoyaiba mod (same ones as for the amplifier)

Tick 20:

- The target takes the full damage of the ability (but no knockback yet)
- The rice_spirit_electric_damage sound effect is played on the target (and for all players within 20 blocks of that target, including the user)
- A vanilla sword sweep particle spawns on the target's center location (halfway between their location and their head location), along with some crit particles

Tick 30:

- The immovable potion effect is revoked from the target
- Flash particles spawn on the target
- 1 lightning spark is spawned on the target, the start position being the target's location at tick 30, and the end location tracking the target's location for the next 15 ticks, the color being secondaryColor, the width being 0.2
- The rice_spirit_knockback sound effect is played on the target (and for all players within 20 blocks, including the user)
- the target gets launched in a random sideways and upwards direction with a large magnitude velocity (using MovementHelper.SetVelocity)
- Target gets the slowness 5 effect for 50 ticks

Tick 31 (cleanup):

- The user can move again, their step height is reset back to default (0.6 blocks), and their animation is cleared

## Third Form: Thunder Swarm

(Lock on) The user dashes right past the opponent, twisting to slash them from behind, this is the first of many, the user retries, essentially teleporting behind the opponent again, slash, tp behind again, slash, over and over, this becomes worse as user's afterimage doubles the attack, so it always looks like the user is attacking the back while the after image attacks the front, and viceversa. These slashes are not very strong, but this form is pretty much impossible to be completely parried or dodged.


Ticks 0-10 (chargeup):

- User plays the iai1 animation
- User cannot move during chargeup
- Call the `MovementHelper.lookAtTarget(entity);` function

Tick 10 (target selection):

- The user selects the nearest target living entity on their crosshair (within 20 blocks)
- If there is no target entity, then the ability cancels (and if the user is a player, then it will send an action bar message to the player saying that you must be looking at a target entity to use this form)
- play the thunder_swarm_dash sound effect halfway between the user's location and the target's location

Ticks 11-20

- User dashes forward towards that position 2 blocks behind the target entity, arriving there by tick 20 (if not before), playing the speed_attack_sword animation

Tick 21:

- Spawn one after image entity two blocks in front of the target entity (if the user is using Kaigaku's sword, then also spawn an after image entity on the left side of the target and also on the right side
- The user teleports to the location 2 blocks behind the target entity
- Create 4 lightning sparks, 2 of them primary color, 2 of them secondary color, placing them in a circular pattern of 2 block radius around the target entity, each lightning spark with a width of 0.07, and they will last for 60 ticks
- play the thunder_swarm_after_image_swarm sound on the target's location

Ticks 21-81:

- User is constantly facing the target entity
- this ability locks on to the target entity, following it every tick, even if the target entity moves away
- Every 8 ticks, the user swaps places with the after image entity (if the user is using the Kaigaku sword, then it will select the after image entity to swap with at random)
- the lightning sparks will circle around the target entity
- every 3 ticks, either the user or the after image (for kaigaku's version, a random after image) will attack, choosing a random attack animation (sword_to_left, sword_to_right, or sword_to_upper), playing the appropriate sword slash visual (through the bone position tracker), and the target will take some damage and knockback
- there will be a spiral of spark particles circling around the target entity

Tick 82 (cleanup):

- Lock on ends, step height is reset, guard state clears, after images and lightning sparks dissapear

## Fourth Form: Distant Thunder

User makes the "Pound #" slashes where he stands, each slash sends a lightning ray forward, these rays flash and blind the opponent and do no damage, but as the opponent is flashed the user dashes towards the opponent and slashes them once, causing the real damage, with a thunder sound. Just like a distant thunder, first you're blinded by the lihgtning then the thunder arrives.

Tick 0:

- The distant_thunder_blinding_slashes sound effect plays on the user, and for all players within 25 blocks

Ticks 0-8:

- User plays 4 different slash animations at triple speed in 2-tick increments, in this order: sword_to_left, sword_to_right, sword_overhead, sword_to_upper
- Every 2 ticks a new lightning spark is created, each one 2 blocks in front of the user's head, in the direction the user is looking, the start location 1 block in one direction, and the end location 1 block in the other direction (so they start with a length of 2 blocks), these sparks have 0.08 width, and their color is primaryColor
- The first lightning spark is right to left (the start position is one block to the right and 0.4 blocks up, the end position is one block to the left and 0.4 blocks up), the second lightning spark is left to right (the start position is one block to the left and 0.4 blocks down, the end position is one block to the right and 0.4 blocks down), the third lightning spark is up to down (the start position is one block up and 0.4 blocks left, the end position is one block down and 0.4 blocks left), and the 4th lightning spark is down to up (the start position is one block down and 0.4 blocks right, and the end position is one block up and 0.4 blocks right), each of them arcs 0.25 blocks forward (in the direction the user is looking)
- During these 8 ticks, these lightning sparks will move with the user, such that they are always in that same relative position to the user, always 2 blocks in front of the user (and the correct length with the proper offsets on the start and end positions), and the arc direction is always in the direction that the user is looking
- And yes the calculations for the locations of these start and end points for the lightning sparks should be local gravity aware

Ticks 8-18

- The 4 lightning sparks move forward (3 blocks per tick), those 8 independent start and end points scaling from their center, all moving away from eachother as the lightning sparks move forward (on a straight projectile path, no longer attached to the user by a fixed offset), their width slowly growing, such that their width is 0.4 by tick 18
  - The 8 independent start and end points should scale from the center of the # symbol (average location between all 8 point), so the symbol that they form should grow in size
  - Their width is also slowly growing (like the width of each lightning spark in the symbol), such that they all reach width 0.4 by tick 18
- Spawn some flash particles every tick randomly between those 8 independent start and end points.
- All living entities anywhere near these lightning sparks (or between these lightning sparks), except for the owner, will get 6 seconds of blindness and slowness 3 and weakness 2 (no potion particles)

Tick 13

- Play the distant_thunder_dash sound effect
- The user's step-up height is temporarily set to 3 blocks
- The user plays the sword_rotate animation

Ticks 13-23

- The user sprints forward (don't play the sprinting animation, just let it do the sword_rotate animation) and let it use the sword slash visual appropriate for the sword_rotate animation (through bone position tracker)
- All targetable living entities within 5 blocks of the user will get damaged and take knockback

Tick 19

- Play the distant_thunder_slash sound effect

Tick 25 (cleanup)

- Clear the user's guard state
- Reset the user's stepheight back to the default (of 0.6)

## Fifth Form: Heat Lightning

Hold pose as if charging power, red zaps all over
The user dashes forward delivering an upwards very powerful slash, which leaves the opponent paralyzed and receiving burning damage even after hit, irradiating those red zaps, the damage is similar than that of stepping in fire.

Tick 0

- Play the heat_lightning_charging sound effect on the user (and for all players within 20 blocks)

Ticks 0-15 (chargeup)

- The user plays the iai1 animation
- 4 red lightning sparks are created, their start position being the user's center (halfway between their location and their head position), the end position being a random solid block within 4 blocks, their width being 0.09, and they should have an upwards arcing vector of 0.7 magnitude (or if there are no nearby solid blocks, then they just go to random points within 4 blocks arcing in random directions rather than up), and those random positions should change and be recalculated every 3 ticks
- The user cannot move during this chargeup phase (but they can rotate)

Tick 15 (direction selection)

- Play the heat_lightning_takeoff sound effect
- Play the speed_attack_sword animation on the user
- A direction is chosen (the direction the user is currently facing
- Temporarily set the user's step-up height to 3

Ticks 15-25

- `B` starts at 0, and increases by 1 every tick, such that `B` is 10 by tick 25
- The user runs forward, traveling 2 blocks per tick
- Every tick a lightning bolt is created, the start location being `(5 - B*0.5)` blocks behind the user, the end location being X blocks in front of and Z blocks above the user's position at that tick. `X = (6-B/2)`, and `Y = (15 * B)`, and yes these location calculations are local gravity aware. These lightning bolts have the colors orange and red (instead of primary and secondary colors)
- All entities caught in the path of any of these lightning bolts will take damage and get marked
- Each of these lightning bolts is a new one, and each one lasts for 20 ticks lifetime

Tick 21

- The user plays the sword_to_upper animation
- Play the heat_lightning_heat_slash sound effect on the user (and for all players within 20 blocks)

Tick 30

- The user plays the iai1 animation and is frozen still for 10 ticks
- Play the heat_lightning_paralyzing_damage on all entities who got damaged (the ones who were marked)
- A flash particle will spawn on all entities who got damaged (the ones who were marked)
- All entities who got damaged will get a red lightning spark on them that lasts for 20 ticks, the start position being their location, and the end position being their head location, the spark following them every tick, with a random arc vector of up to 0.6 blocks in magnitude, their start and end location having a random offset of up to 0.2 blocks in magnitude, the width being 0.08, and the color being red
- All entities who got damaged will get slowness 3 for 5 seconds (and if this is using Kaigaku's sword, or the user is a demon, then targeted entities will also get set on fire for 5 seconds, and nearby solid blocks will be ignited on fire)

Tick 35 (cleanup)

- The user's guard state is cleared
- The user's step height is reset back to the default (0.6)
- The user is unfrozen
- The user's animation is cancelled/cleared


## Sixth Form: Rumble and Flash

The user holds a pose as black smoke starts to envelop them, then the user dashes forward enveloped in a cloud of dark smoke, delivering a single very strong, explosive sideways cut, 360° spin while cutting (similar to "rings of ember"), small lightnings appear on the cloud, then big ones irradiate from the sword strike, with higher range, the hitbox extends forward, the sound is like an explosion of thunder.

Tick 0

- Play the rumble_and_flash_charging sound effect on the user (and for all players within 20 blocks)

Ticks 0-20 (chargeup)

- The user plays the iai1 animation
- The user cannot move (but they can look around)
- The user is surrounded by dark smoke particles (use both smoke particles and black dust particles on size 4, being spawned within 3 blocks of the user)

Tick 20 (aim and takeoff)

- Play the rumble_and_flash_takeoff sound effect on the user (and for all players within 20 blocks)
- Temporarily set the user's step height to 3 blocks

Ticks 20-30 (dash phase)

- User dashes forward 15 blocks in 10 ticks, trailing a dense cloud of dark cloud particles (use smoke particles and also black dust particles on size 4)
- These particles should be arranged in circular rings around the user, the circle's axis being the user's look direction, centered on the user's center (halfway between their position and their head, spawning a ring of smoke particles and black dust particles every tick, the ring being a 2.5 block radius)
- The user plays the side_flip animation
- Create one lightning spark every tick, half of them should be primary color, the other half should be secondary color, and they should be flickering, and their width should be somewhere random between 0.05 and 0.09
- These particles that spawn should continue to spawn for the next 20 ticks, essentially remaining as a tube of dark particles
- All the lightning sparks should each get random start and end positions within this tube (their start and end positions being recalculated every 2 ticks to another random location), and their arcing vectors being random vectors of maximum magnitude of 1, and they should all have a lifetime between 40 and 50 ticks
- All targetable entities that are within 3 blocks of the path of this dash will get damaged

Tick 30

- Play the rumble_and_flash_strong_slash sound effect on the user (and for all players within 25 blocks)
- The user plays the sword_rotate animation
- Flash particles are spawned on the user

Ticks 30-40

- User is frozen
- All the lightning sparks (created during the dash phase) will have their width get 60% larger
- The start positions of the lightning sparks will continue to choose random locations within that tube of smoke (and the smoke particles and black dust particles will continue)
- The end positions of the lightning sparks will choose a random point up to 5 blocks in front of the user (with a random offset of up to 5 blocks in any direction), with a random arcing vector (maximum magnitude of 2)
- These start and end positions will continue to be recalculated every 2 ticks, randomized

Tick 40

- Play the rumble_and_flash_leftover_lightning on the user
- All of the lightning sparks (the ones that are still alive, because the lifetimes might have expired for a few by now), their width should be divided by 2

Ticks 40-50

- The lightning sparks start and end positions should be randomly around the tube of smoke (the smoke particles have stopped spawning, so they are clearing out now), with random arcing vectors of a maximum magnitude of 1

Tick 50 (cleanup)

- Clear the animation on the user
- Reset the user's step-up height to the default (0.6 blocks)
- Clear the user's guard state
- User is unfrozen

## Seventh Form: Flaming Thunder God

Charging pose, huge lightning irradiates from user, slow, growing, silent, here the user should aim carefully, the user then travels great distance at tremendous speed, with a single sideways cut delivered, enormous damage, one shot for not very strong opponents, big cooldown, can be performed wherever the user is aiming, either horizontally or vertically.
Visually a flaming, yellow huge long dragon of thunder follows the user's trajectory, the after image of that dragon stays for a couple seconds after the hit was delivered (just to be appreciated, for due to the tremendous speed of the user, youll hardly be able to see it in movement). Downside: The user is paralyzed a few seconds after performing the attack, due to the great power output
This form is ideal for an opponent at big distance, the closer you are, the more chance they'll have to get out of your corsshair and dodge it, the further away, the better chance you have, for you must aim with your crosshair, and the hitbox of the slash is wide, but if the attack misses, it leaves you sold...

Gravity:

- If a gravity mod is installed, this form manipulates the user's gravity, and that gravity should override any gravity fields that the user might be in or pass through for the entire duration of this form

Tick 0:

- Play the thunder_god_charging sound effect on the user (and for all players within 20 blocks of the user)
- Do a 5 block raytrace from the user's location downwards, to find the nearest solid block, and select the blockface that the raytrace hits
- If that raytrace doesn't hit any blocks, then do a 5 block raytrace from the user's location in the positive X direction, if that doesn't hit anything, then do one in the negative X direction, if that doesn't hit any blocks then try the positive Z direction, then the negative Z direction, and then finally the positive Y direction (so essentially it should be finding the nearest solid block in one of the cardinal directions, straight along one of the axis lines)
- If the raytraces don't find any solid blocks, then the user will be hovering during the chargeup
- If a raytrace found a block, then for the duration of this form, the user's gravity direction will be set to the direction of that raytrace (this affects the up and down and left and right direction and coordinate calculations for the duration of this form)
- If there is no gravity mod installed, then it ignores the gravity stuff and it just does the one downward raytrace to find the block underneath the user, if the downwards raytrace doesn't find a block below the user, then the user will be hovering during the chargeup
- Create 6 lightning sparks, each one has a width of 0.15, and each one is of color secondaryColor, and 2 of them are flickering

Ticks 0-70 (chargeup):

- R starts at 1, and it increases continuously, such that R reaches 6 by tick 60
- Play the iai1 animation continuously
- User cannot move (but the user can rotate)
- If the user is not hovering, the moves downward toward that blockface that the raytrace sequence from tick 0 found (so the user moves downward in their custom gravity direction that was set during tick 0
- For each of the 6 lightning sparks, every tick, their start location is set to a random location between the user's location and the user's head location (local gravity aware)
- Every 2 ticks, the end position of every spark will have a 60% chance of being updated. When the end position is updated, it should find a block within a `R` block radius that doesn't already have a spark connected to it (if it can't find a block, then it just chooses a random spot in the air within `R` blocks), and if it does find a block, then it connects the end position to a random spot within that block, and it sets the arc vector of that spark to a vector with the user's local upwards direction (depending on the user's gravity direction, so like this should be gravity aware), and the spark's arc vector should have a random magnitude between `R * 0.2` and `R * 0.5`, and the width of the spark should be set to `R * 0.05`
- A spiral of spark particles should be swirling around the user during this charge up too, this charge up should look legendary

Tick 30

- 6 more lightning sparks should be created, their color should be primaryColor, and they should behave just like the other lightning sparks, but for them they use 0.5*R instead of R, so they have half the width and half the range, but other than that they should behave the exact same as the 6 secondaryColor lightning sparks until tick 70

Tick 40

- 10 more lightning sparks should be created, these are the 8 traveling lightning sparks, they are of color secondary color, 0.1 width, and they will travel along blockfaces
- These 10 traveling lightning sparks will start arranged in a circle around the user (horizontal circle of 4 block radius, horizontal according to the user's current gravity direction, so the axis of rotation of this circle is the user's local upwards direction, gravity aware), and their arc vectors should be upwards (the user's local up), their end positions will each be 3 blocks further out

Ticks 40-70 (traveling sparks)

- Every 2 ticks, the 10 traveling lightning sparks will look for their next target, if there is a nearby living entity within 4 blocks (that hasn't been hit by a traveling spark yet), then it will target that living entity (the arc vector being that entity's local up according to their local gravity), and it will give that entity slowness 5 for 3 seconds (no potion bubble particles). Otherwise if there is no nearby living entity within 4 blocks that hasn't been hit by a traveling spark yet, then it will find a different block (other than the ones that the start position and end position are currently on) within 5 blocks of the current end position (choose a random one, but favor the ones further away from the user), and choose a blockface (ideally one that faces air, otherwise just select the center of the block). Once the target location is acquired, then it will set the current start location to the current end location, and then it will move the current end location over to the target location (leapfrogging, essentially the start location goes where the end location used to be), and the arcing vector will be set to the direction of the blockface on the end location (or if the end location is a living entity, it will be that entity's up direction depending on their gravity), or if there is no blockface on the end location, then it will just set the arc vector to a random direction. And the arc vector will have a random magnitude between 0.8 and 1.4
- These traveling sparks will need to be optimized somehow, such that they don't create lag

Tick 70:

- Raycast up to 80 blocks forward starting from the user's head in the direction the user is looking
- Select the location on that raycast that intersects the first living entity or solid block
- That location is the target location

Tick 71 (launch):

- Play the thunder_god_ultimate_slash on the user and for all players within 50 blocks of the user
- Create a lightning bolt with the start location being 10 blocks behind the user's start location, and the end location should track the user, for this lightning bolt the colors should be flipped, the secondaryColor should be on the start side of the gradient, and the primaryColor should be on the end side of the gradient
- Create 5 lightning sparks, 3 of color primaryColor, and 2 of color secondaryColor, their start location should be 10 blocks behind the user's start location, and their end location should be within 2 blocks of the user's location (random offset up to 2 blocks, updating every tick to follow the user and be at a different random offset), and they should have a random arc vector of a magnitude up to 2.5 blocks, and their width should be 0.35

Ticks 71-74 (first stage):

- The user zooms 60% of the distance to the target location, such that they arrive at that 60% mark right at tick 74
- The end location of the lightning bolt and the 5 lightning sparks should update to follow the user (the lightning sparks can have a random 2 block offset)

Tick 74 (create the thunder dragon):

- Create the thunder dragon (we will have a thunder_dragon model for a geckolib entity), the thunder dragon should share the exact same rotation and location and gravity as the user

Ticks 74-76:

- User continues playing the iai1 animation
- The lightning bolt and the lightning sparks continue to render
- The user hovers frozen in mid-air for these couple ticks
- Flash particles spawn around the user

Tick 77:

- User plays the sword_to_right animation
- The user teleports to the target (if the target is a living entity, then the user teleports to that living entity)
- The dragon also teleports to the target, along with the user
- The lightning bolt and the lightning sparks continue to render, their end positions updating to follow the user
- All valid target living entities within 8 blocks of the target take the full damage of the form. This form should do 40 damage through the Damager.hurt() function, with the user as the damage source. All living entities that get damaged will have flash particles spawn on them
- If the user is a demon, then the living entities that get damaged will also get set on fire for 60 ticks

Tick 78-88:

- Thunder Dragon is no longer tracking the user, it is now slowly moving in the user's direction
- Flame particles spawn around the user
- The user gets a large velocity in the same direction that they just traveled (the direction between the start location and the target location)

Tick 88:

- User gets weakness 10 for 3 seconds (no particles)
- User gets mining fatigue 10 for 3 seconds (no particles)
- The lightning bolt and lightning sparks are removed

Ticks 89-149:

- User plays the iai1 animation on repeat
- User cannot move (but they can look around)
- Smoke particles spawn around the user

Tick 120:

- Thunder Dragon is removed in a cloud of smoke particles

Tick 150 (cleanup):

- Cleanup, user is unfrozen, animation stops
- Guard state is cleared
