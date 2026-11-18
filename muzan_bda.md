# Biokineses

Muzan's blood demon art is called "Biokineses". This is a legendary blood demon art.

**Brambles**

Brambles are similar to Lightning Sparks. However they do not glow, and they are just the outer beam, no inner beams, and no glowing either.

Brambles don't take an array of positions, they just take a start position and an end position as inputs, along with a width (in blocks). The width is a float, so it can be less than a block. The inputted width refers to the width of the outer segment (there is no inner segment, unlike the Lightning Sparks). It also takes in two colors (default red and black). The Bramble is also given a tick lifetime as an input. Also, the start position and end position of the brambles should be changeable. And it should also be possible to give the Brambles a velocity vector (the velocity vector gets added to both the start position and the end position on every tick).

Brambles also takes in an optional arcing vector. The arcing vector (default 0 0 0) provides both a direction and magnitude to arc the spark segments.

A Bramble is just one beam with 100% opacity and no glow

- The beam is two colors, split lengthwise down the middle

A Bramble can have multiple segments depending on its length and width. If the length squared of the segment (length squared because it's distance between start position and end position, and square roots are expensive, so just multipy the coefficients by eachother to avoid doing any power functions), if the length squared of the segment is less than 25x the width, then there will only be one segment. If the length squared is 25x the width, then there will be two segments. So ergo the number of segments equals the length squared divided by 25 multiplied by the width (length squared is in numerator, the width multiplying 25 is in the denominator). There will be a minimum of 1 segment, and a maximum of 50 segments (let's guard for divide by 0 errors).

If there is a non-zero arcing vector, then each of the segments will arc in the direction of that arcing vector, depending on proximity to middle. So like the edges (start position and end position) aren't affected at all. The middle position is shifted by 100% of the arc vector, so for that position in the middle of the segments, the arc vector should be added directly to it. For the position at 25% through the segments, and 75% through the segments, half of the arc vector should be added.

The first segment starts at the start position of the Bramble. The last segment ends at the end position of the Bramble. The positions of all the other points for the segments are calculated every other tick (so half are calculated on one tick, the other half are calculated on the other tick). These intermediate point locations are just calculated locally, like in rendering, no need to sync this with server, as it's just graphics. They are calculated by arranging the points sequentially in a straight line between the start position and end position, and then adding a random offset to every point in a random direction, the offset magnitude being a maximum of 5\*width.

Between each segment, like at the joint between segments, the segments should overlap eachother slightly, each segment should go slightly past the joint

**Blood Tornado**

- The blood tornado is an entity/model, it's an asset that resembles a spinning red and black tornado
- It deals damage to all nearby entities, and it catches nearby projectiles

**Muzan hands**

- The biokineses_0 item (and the biokineses_0l rendered in the left hand, if the ability calls for it)

**Flesh Arm**

- The biokineses_1 item (and the biokineses_1l rendered in the left hand, if the ability calls for it)
- this is an animated Geckolib item

**Arm Blade**

- The biokineses_2 item (and the biokineses_2l rendered in the left hand, if the ability calls for it)
- this is an animated Geckolib item

**Flesh Vines**

- A thick rendered tentacle, similar to the back tentacles, but much thicker, with the flesh vine texture
- the flesh_vine_tip model is rendered at the end of the flesh vine, facing the direction that the end is facing
- the start of the flesh vine can either be connected to a location (which can move), it it can be connected to one of the bones of the user (like the torso or the arm, similar to the back tentacles)
- the flesh vine should have a little bit of randomness in its path, and it should be quite lumpy, the segments varying in thickness somewhat randomly, with some lumps and protrusions here and there, and they should be wriggling and writhing
- The flesh vines will also need an implementation to handle a custom defined path, with an array of points to define that path (like for the sinusoid motion of ultimate authority)

**Bone Whips**

- A thinner version of the flesh vine, with the bone_whip texture
- The bone_whip_tip model is rendered at the end of the bone whip, facing the direction that the end is facing
- The start of the bone whip is connected to an entity (either connected to an entity with an offset), or connected to a specific bone of that entity, either their left arm or their right arm
- The bone whip has a little bit of randomness in its path, and it should be quite lumpy, the segments varying in thickness somewhat randomly, with some lumps and protrusions here and there, and they should be wriggling and writhing

## Black Blood, Brambles

_Muzan hands_

Muzan lifts and aims his left forearm towards his target, before uses his blood from his veins, that creates a red-and-black veins-like cracks, to create an array of thorny, thin tendrils to entangle and slash his target.

Tick 0

- Biokineses item uses the biokineses_0 model (and renders biokineses_0l on the left hand)
- Play the brambles animation (lasts until tick 40)

Ticks 0-17 (chargeup phase)

- User can only move at 0.25x speed
- Charging up the blood brambles
- Tiny black and red swirling dust particles (size 0.1) centered at the user's head position shifted 0.5 blocks forward and 0.4 blocks to the left (from the user's perspective, local gravity aware)

Tick 18

- Create 5 brambles, red and black in color, and 0.05 to 0.1 in width (random between those)
  - 1 bramble is black only, 1 bramble is red only, and the rest of both red and black

Ticks 18-60

- Set brambleStartPos to the user's head position shifted 0.8 blocks forward and 0.3 blocks to the left (from the user's perspective, local gravity aware),
- Each bramble has the start position set to brambleStartPos shifted by a random vector of magnitude up to 0.06 (updated every tick)
- Every tick, increment brambleRange by 6 blocks, up to a maximum of 60 blocks
- Each bramble will have a different randomized end position, to calculate that position, take the start position, add the user's forward vector multiplied by brambleRange, and then shift it by a random vector in a random direction of a magnitude up to a maximum of a third of brambleRange
- That randomized end position will be recalculated every tick (so no point in storing them)
- Each of the brambles should get a random arcing vector of magnitude up to a maximum of a fifth of brambleRange (recalculated every tick)
- Place a blackred_slash particle at the end position of every bramble
- All living entities in that cone (length is brambleRange, radius is a third of brambleRange) will take damage every 2 ticks (and yes that should ignore/reset the damage cooldowns, using our Damager.hurt function), also whenever a living entity is damaged by this ability, it should get some blackred_impact particles on it
- Every 2 ticks, raycast forward a full 60 blocks, at every solid block that intersects that raycast, place a blackred_slash

## Merciless Punishment

_Muzan Hands_

_Muzan Hands_

The user swipes his left arm, creating a violent storm of red scratches with his sharp fingernails, and unleashing a blood tornado.

Tick 0 (setup)

- Biokineses item uses the biokineses_0 model (and renders biokineses_0l on the left hand)
- Plays the left_arm_overhead animation with the red claw swing and AOE damage nearby
- Sets up the guard state
- Spawns a blood_tornado entity and is launched forward as a projectile that travels along the ground

Ticks 0-20

- The user moves at half movement speed
- The blood_tornado entity travels along the ground, continuing to move forward 2 blocks per tick

Tick 21 (cleanup)

- Form cleanup, the animation on the user is cancelled, the guard state is removed, the user is no longer slowed
- The blood_tornado entity is removed

## Ultimate Authority

_Flesh Arms_

The user stomps on the ground and a line of upwards flesh vines surges forward, give of them in a line rolling forward like a tidal wave of destruction.

Ticks 0-15 (charge up)

- Biokineses item uses the biokineses_1 model
- play the right_arm_chargeup animation on the user
- User moves at quarter speed and can't jump, user is hovering during this time, 3 blocks higher than where they started the form from (they will float up within the first 5 ticks of the charge up, and this "up" is local gravity aware)
- Create 3 flesh vines on the user's location
- The start location of all 3 flesh vines should be one block below the user's location (local gravity aware), and the end location should be 2 block's above the user (local gravity aware) and a random 1.5 block offset in any direction that is perpendicular to the upward vector (the upward vector in the local gravity)
- The start and end location of all 3 of those flesh vines should be recalculated every tick (with a new random end location each tick)

Tick 17

- Render the biokineses_1l model on the left hand from tick 17 until the form cleanup
- User slams downward while playing the heel_drop animation, and the attack happens as soon as the user hits the ground (if the user doesn't hit the ground in the next two seconds, then the ability is cancelled)

When the user hits the ground:

- Calculate a 15 block horizontal line perpendicular to the user's facing direction (7.5 blocks to the left, 7.5 blocks to the right)
- Calculate 5 evenly spaced points along that line (3 blocks apart)
- on each of those 5 points, there will be a flesh vine that spawns, the start location being the nearest solid block underneath that point (do a 1 block offset below that solid block), and the end position being 1 block higher than the starting position, facing vertically

After the user hits the ground and those 5 flesh vines are created

- The 5 vines travel forward in a large sinusoidal path, using a cosine function for the Y position, the vines themselves should take the shape of that sinusoid, as more of the cosine function gets traced out as ticks go on
- The cosine function has an amplitude of 15 blocks
- A damaging AABB box follows the end position of every one of these vines, using the Danaher.hurt function (using the user, the owner of these vines, as the damage source) on all (non-owner) living entities that intersect this AABB box (and they have blackred_impact particles spawned on entities that get damaged, and there will be explosion particles on the ends of these vines whenever the AABB box hits a full block)

Form cleanup:

- Clear the user's animation and guard state

## Black Blood, Descendence

_Muzan Hands_

The user leaps into the air, and then zooms to their target at immense speed, unleashing a ground pound, with red and black brambles shooting out of the ground around the groundpound location.

Ticks 0–20 (Charge-up)

- Biokineses item uses the biokineses_0 model, rendering biokineses_0l on the left hand.
- Play the right_arm_chargeup animation.
- User hovers 20 blocks upwards (and 10 blocks backwards), taking 10 ticks to get there and hovering for 10 ticks
- User movement is reduced to 0.25x speed.
- Spawn small black and red swirling dust particles around the user's body.

Tick 20

- Select the target point (raycast up to 40 blocks from the user's head (in the direction the user is looking), choosing the point at the first intersecting solid block, if no blocks are found, then it will raycast downwards until a solid block is detected)

Ticks 20-25

- The user zooms towards that target location, such that they hit the target location on tick 25
- Play the heel_drop animation

Tick 25

- Deal AOE damage to all living entities within a 6-block radius.
- Apply strong knockback away from the impact point.
- Create an explosion of blackred_impact particles.
- Spawn 10 brambles in a 6 block radius around the user, coming out of the ground, shooting 30 blocks upwards and wriggling around, the tip moving around randomly with a horizontal displacement of up to 6 blocks, and some random arcing vectors of up to 3 blocks in magnitude
- All entities within the path of those brambles take damage and will get the blackred_slash particles on them
- These brambles stay for 30 ticks

Ticks 25-55

- The user is moving at 0.25x movement speed

Tick 56 (cleanup)

- Animations are cleared, guard state is cleared

## Forced Transition

_Flesh Arms_

The flesh vines Upon making contact with a convertible mob begins the demonstrating of the entity, with a 50-50 chance of the process killing the target

Tick 0:

- Biokineses item uses the biokineses_1 model
- Choose a target entity or location within 80 blocks (80 block raycast forward, select the nearest living entity to the crosshair, or select the block that the raycast intersects, or select the point at the end of the 80 blocks)

Ticks 1-40:

- Play the right_arm_forward animation on loop
- User moves at half movement speed
- A flesh vine with the starting point attached to the user's right arm starts moving forward, towards the target (so like the end position is always facing the target entity/location, and moving towards, slithering through the air randomly to get there)

When the flesh vine hits a target entity, it latches on to the target and stays attached to that target entity for 20 ticks.

- The target entity that the flesh vine is attached to takes 30 damage (using the Damager.hurt with the user / flesh vine owner as the damage source, so it can be scaled) every 10 ticks
- If the user drops to zero HP during this ability from this damage (and they are a demonizeable entity or a player), then there is a 50% chance of instantly turning them into a demon (no demon transformation phase, just instant demon transformation, and if it's a player then it will instantly forcefully feed them 10 to 20 muzan blood, if the player is already a demon then it does nothing, it just lets them die)
- If the user survives this attack, then they get 10 seconds of wither 5, and also slowness 3 for 10 seconds

Tick 50 (cleanup):

- Clear the animation on the user

## Shockwave Energy Blast

The user releases a long-ranged shockwave that severely damages the nervous systems of those caught in it, causing seizures, convulsions, and even rendering them unable to breathe for a brief period of time due to damage inflicted to the target's lungs. He uses it by morphing his torso into a gigantic beastly mouth to release it in all directions.

Tick 0

- Play the shockwave animation on the user
- Play the minecraft:entity.warden.sonic_charge sound effect at 0.75 pitch
- Play the minecraft:entity.guardian.attack sound effect at 0.75 pitch

Ticks 0-12 (chargeup)

- 3 flesh vines will spawn, each one as a circle of radius R, centered on the user's center (halfway between their location and their head location) and circling rapidly around the user's center, with a random orientation that changes every tick
- User moves at 0.25x speed during this chargeup phase
- R starts at 4, and then decreases to 0 by the 12th tick

Tick 13

- Spawn 10 lightning sparks (yes the same ones from thunder breathing), they should each have a width of 0.8, and they should just be entirely white in color
- Play the minecraft:entity.warden.sonic_boom sound effect
- Play the thunder sound effect
- Remove the 3 flesh vines
- R starts at 1

Ticks 13-26

- R increases every tick, reaching 50 by tick 26
- Every tick, spawn a sphere of radius R of flash particles around the user
- For each lightning spark, the start point is set to the user's center location (halfway between their head and location), and the end point is a random point along the edge of the sphere of radius R centered on the user (same sphere as the flash particles)
- For every living entity (other than the user) within R blocks of the user (use the distance squared trick to avoid the square root issue, comparing to R\*R, such that we don't have to use the slow power function), once they first enter the region, they become marked, meaning a new lightning spark will spawn on them (lasting 10 ticks), start point being the user's center position and end point being the living entity's center position (don't keep spawning more lightning sparks on the same living entity over and over again, if a living entity already has a lightning spark from this ability attached to them then just update the start and end positions every tick such that it tracks both entities)
- All living entities who get hit by this ability (they are within R of the user) will take a bunch of damage, and then immediately get slowness 20 for 5 seconds (no particles), and they will have the kimetsunoyaiba lightning particles on them for that duration, and they will also have the suffocation potion effect (from kimetsunoyaiba mod) for 5 seconds, and it will cancel any active abilities or breathing forms they have active (also any entity who has the suffocation effect shouldn't be able to use abilities, they cant use blood demon arts or breathing forms while they have the suffocation effect), and their velocities will also be instantly zeroed when they get hit by this ability

Tick 30 (cleanup)

- Give 5 seconds of slowness 10 to the user (no particles)
- Clear the guard state and the animations on the user

## Blades of Disaster

_Blade arms_

The user grows long blade-like arms (or the bone whips), crossing them and unleashing them in a large crisscross slice, sending an aftershock of blood tornados that launch forward half a second later that cleaves through everything in its path

Tick 0

- Biokineses item uses biokineses_2 item (and the biokineses_2l is rendered on the user's left hand)
- User plays one of the beast breath animations (the one with the criss crossing arms that does two slashes at once)

Ticks 0-8

- 2 bone whips spawn, each one has a start position on one of the arms (one on the right arm, one on the left arm), and each one is 30 blocks long and slightly curved
- Both bone whips spin around the user 90 degrees in opposite directions, starting from a 30 degree offset from where the user is looking
- Both bone whips are arcing in the same direction they are rotating
- 2 crisscrossing arcs of blackred_slash particles launch forward from the user (forming a curved X shape, getting larger as they go further from the user, traveling rapidly in the direction the user is looking, doing damage to all living entities in their path)

 Tick 15

 - 6 blood tornados launch forward from the user, each traveling at a speed of 2 blocks per tick, in the direction the user is looking, each one trailing blackred_impact particles, they launch forward at degree offsets, 7 degrees apart from eachother in trajectory angle 
 - The blood tornadoes travel for 40 ticks before dissapearing in a cloud of explosion particles

Tick 30 (cleanup)

- Bone whips are removed
- User's animation and guard state are cleared

## Vengeful Abyss, Fissure

_Flesh vine, Tentacles_

The user extends a large fleshy arm in a straight line forwards, mottled with mouths. The mouths suck air inwards, pulling all nearby entities towards the arm. Arm then unleashed bursts of paralyzing energy bolts while the user's back tentacles and leg tentacles surge forward and decimate all the ensnared entities

Tick 0:

- Biokineses item uses the biokineses_1 model
- Select the target location 60 blocks forward from the user's head, in the direction the user is looking

Ticks 1-10 (flesh vine extension):

- Play the right_arm_forward animation on loop
- User moves at half movement speed
- A flesh vine with the starting point attached to the user's right arm starts moving towards that target location, (in the direction the user is looking, the end position is always facing forward, moving in a mostly straight line)
- The path of the flesh vine arcs between the user and the end point (similar to a bezier curve, with the control point being up to 20 blocks in front of the user (depending on the tick, it starts at 1 block in front, and then it's 20 blocks in front by tick 20)
- By tick 10 the flesh vine will reach the target location

Ticks 10-40 (flesh vine suction):

- Continue playing the right_arm_forward animation on loop
- The user moves at half movement speed
- The start position of the flesh vine is attached to the user's right arm
- The end position of the flesh vine stays at the target location
- The path of the flesh vine arcs between the user and the end point (similar to a bezier curve, with the control point being up to 20 blocks in front of the user (depending on the tick, it starts at 1 block in front, and then it's 20 blocks in front by tick 20)
- 20 flesh mouths will be placed (evenly spaced) along the path of the flesh vine (and they will follow the flesh vine as it moves and twists, staying on their sparts on the flesh vine)
- On each of these flesh mouths, an inhale particle will be attached, following its respective flesh mouth
- All entities (except for the user) within 20 blocks of a flesh mouth will be pulled towards that flesh mouth (using MovementHelper.AddVelocity), and if kimetsu block break gamerule is enabled, then every 10 ticks a random (non-air and non-fluid) block within 10 blocks of each flesh mouth will turn into a falling block and get pulled towards the flesh mouth too. The closer an entity is to a flesh mouth, the stronger that suction force (the velocity pulling the entity towards the flesh mouth) is

Tick 41:

- User plays the heel_drop animation
- 30 flash particles spawn along the path of the flesh vine
- 10 white lightning sparks spawn along the path of the flesh vine (their width is 0.1), their starting points being on the flesh vine, their ending points being a random spot 5 blocks away, their arcing vectors being a random vector of magnitude up to 2
- All living entities within 5 blocks of the flesh vine's path (or within 6 blocks of a flesh mouth, if that's easier) immediately get slowness 20 for 5 seconds (no particles), and they will have the kimetsunoyaiba lightning particles on them for that duration, and they will also have the suffocation potion effect (from kimetsunoyaiba mod) for 5 seconds, and it will cancel any active abilities or breathing forms they have active (also any entity who has the suffocation effect shouldn't be able to use abilities, they cant use blood demon arts or breathing forms while they have the suffocation effect), and their velocities will also be instantly zeroed when they get hit by this lightning spark flash
- Play the minecraft:entity.warden.sonic_boom sound effect on the user and for all players within 30 blocks of the user or the flesh vine's end position
- Play the thunder sound effect on the user and for all players within 30 blocks of the user or the flesh vine's end position
- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)

Ticks 41-51

- The user's movement speed is reduced to 0.1x
- The back tentacles zoom forward, following the path of the flesh vine (with a random offset of up to 3 blocks), and reaching the end position of the flesh vine (the target position) by tick 51
- Every tick, play a blackred_slash particle on the end position of every tentacle
- As the tentacles zoom forward, all living entities in their path (or within 5 blocks of the flesh vine path, or within 6 blocks of the flesh mouths) take damage (and get the blackred_impact particles on them) 

Ticks 52-62 (retraction)

- The flesh vine and the back tentacles all retract back towards the user

Tick 50 (cleanup):

- Flesh vine is removed
- User's movement speed is reset back to normal
- Clear the animation on the user
- The right_arm_forward animation ends
- Back tentacles retract and go back into default standby mode

## Blood King’s Massacre

_Flesh Arms_

Unleash multiple bone whips in a 50 x 50 area ripping apart and tearing apart opponents, dragging the disemboweled corpses back to be consumed by their body for high regeneration, can only be Blocked or negated by hashira or high tier Demon slayers, leaves lasting high-level poison on the survivors, slowly poison their bodies as the fight drags on

Tick 0

- Biokineses item uses the biokineses_2 model, and biokineses_2l is rendered in the left hand

Ticks 0-10 (chargeup)

- Play the summon animation while particles spiral around the user, user moves at 0.5x speed

Ticks 11-53

- User moves at 0.25x speed

Tick 11 (first slash)

- Spawn flash particles on the user
- Spawn a circle of 12 outwards-facing blood lances around the user at a 2 block radius, launching outwards horizontally
- 1 bone whip is created, the starting position anchored to the user's left arm, the end position and the path spiraling around the user counterclockwise horizontally with a bit of wobble and randomness (in a 5 block radius)
- User plays the left_sword_to_left animation

Tick 17 (second slash)

- Spawn flash particles on the user
- A 2nd bone whip is created, the starting position anchored to the user's right arm, the end position and the path spiraling vertically around the user with some wobble and randmoness (in an 10 block radius)
- User plays the sword_overhead animation
- Spawn a ring of 12 additional blood lances around the user, the circle is a 8 block radius, and the directions of these blood lances are tangential to the circle, these ones should move in a circular path (that 6 block radius circle) around the user

Tick 23 (stomp)

- Spawn flash particles on the user
- The user plays the heel_drop animation
- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)
- Spawn a third ring of 12 more blood lances around the user, the circle is a 18 block radius, and the directions of these blood lances are tangential to the circle, these ones should move in a circular path (that 18 block radius circle) around the user

Ticks 23-53:

- User moves at 0.25x speed
- 25 block radius centered on the user's head
- The six back tentacles will whip around randomly in a 25 block radius from the user
- The two bone whips continue to thrash and whip around in a 15 block radius
- Spawn a bunch of blackred_slash particles randomly within this 25 block radius sphere
- All entities within 25 blocks, every 5 ticks they have a 50% chance of getting hit, those that get hit will get 10 seconds of wither 5 potion effect
- Those that get hit get the blackred_impact particles on them and also get pulled towards the user (using MovementHelper.AddVelocity)
- Entities within 10 blocks of the user will get marked by size 4 red dust particles on them while an extra flesh vine is created connecting them to the user (start point is on the user's torso, end point is on the target's torso), and this flesh vine will thrash them around and drag them closer and closer to the user, these marked entities taking 50% more damage from the form, and every time they take damage, half of the damage they take goes towards healing the user

Ticks 54-69 (retraction)

- The user moves at 0.1x speed during this retraction phase
- R starts at 25 on tick 54, and decreases to 1 on tick 69 (the radius of the sphere around the user's head)
- The 6 back tentacles and all the flesh vines retract (their endpoints right on the edge of the sphere of radius R) as they slither back towards the user

Tick 70 (cleanup)

- User's animations and guard state are cleared
- All the flesh vines are removed
- Back tentacles are put back into a default state

## Blood King’s Massacre, Genocide (variant of Blood King's Massacre)

_Flesh Arms, Tentacles_

Unleash multiple flesh vines in a 50 x 50 area ripping apart and tearing apart opponents, dragging the disemboweled corpses back to be consumed by their body for high regeneration, can only be Blocked or negated by hashira or high tier Demon slayers, leaves lasting high-level poison on the survivors, slowly poison their bodies as the fight drags on

Tick 0

- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)
- Biokineses item uses the biokineses_1 model, and biokineses_1l is rendered in the left hand

Ticks 0-10 (chargeup)

- Play the summon animation while particles spiral around the user, user moves at 0.5x speed
- The back tentacles will be spiraling upwards (above the user, local gravity aware)

Ticks 11-61 

- User moves at 0.25x speed
- 25 block radius centered on the user's head
- 4 flesh vines whips (two attach to each arm on the user) will whip around randomly in this 25 block radius sphere, their endpoints being right at the edge of the 25 block radius, with some large random arcing vectors
- All 6 of the back tentacles will also whip around randomly in this 25 block radius sphere
- Spawn a bunch of blackred_slash particles randomly within this 25 block radius sphere
- All entities within 25 blocks, every 5 ticks they have a 50% chance of getting hit, those that get hit will get 10 seconds of wither 5 potion effect
- Those that get hit get the blackred_impact particles on them and also get pulled towards the user (using MovementHelper.AddVelocity)
- Entities within 10 blocks of the user will get marked by size 4 red dust particles on them while an extra flesh vine is created connecting them to the user (start point is on the user's torso, end point is on the target's torso), and this flesh vine will thrash them around and drag them closer and closer to the user, these marked entities taking 50% more damage from the form, and every time they take damage, half of the damage they take goes towards healing the user
- Any living entities that are going to be killed by this damage will get launched towards the user (using MovementHelper.SetVelocity()) at great speed while repeatedly spawning size 4 red dust particles on them and blackred_impact particles on them (and the particles continue traveling towards the user, the red dust particles gradually getting smaller in size as they approach, to a minimum size of 1.5), and then once those red dust particles reach the user, the user heals 5% of their max HP and gains a level and +10 seconds of the killing intent potion effect

Ticks 62-77 (retraction)

- The user moves at 0.1x speed during this retraction phase
- R starts at 25 on tick 67, and decreases to 1 on tick 82 (the radius of the sphere around the user's head)
- The 6 back tentacles and all the flesh vines retract (their endpoints right on the edge of the sphere of radius R) as they slither back towards the user

Tick 78 (cleanup)

- User's animations and guard state are cleared
- All the flesh vines are removed
- Back tentacles are put back into a default state

## Demon King’s Wrath

_Blade arms, tentacles_

Unleash multiple flesh and bone vines that tear apart the battlefield and also cause aftershock of energy to tear up whatever is left, apart, trees, buildings, and even the hardest of stones nothing is safe besides Hashira who can dodge in time but surviving a hit from this does not mean much because it also inflicts high intensity poison that can once the health of the opponent mobs are low enough either kill or turn them into a demon against their will without being treated but the ability is followed up by consistent whipping slashes!

Tick 0

- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)
- Biokineses item uses the biokineses_2 model, and biokineses_2l is rendered in the left hand

Ticks 0-15 (chargeup)

- Play the summon animation while particles spiral around the user, user moves at 0.5x speed
- The back tentacles will be spiraling upwards (above the user, local gravity aware)

Ticks 16-66 

- User hovers during this ability (no gravity)
- Play the minecraft:entity.warden.sonic_charge sound effect at 0.75 pitch
- Play the minecraft:entity.guardian.attack sound effect at 0.75 pitch
- User moves at 0.25x speed
- 25 block radius centered on the user's head
- 4 bone whips (two attach to each arm on the user) will whip around randomly in this 25 block radius sphere, their endpoints being right at the edge of the 25 block radius, with some large random arcing vectors
- All 6 of the back tentacles will also whip around randomly in this 25 block radius sphere
- At the endpoints of each bone whip and each back tentacle there will be a blackred_slash particle
- Spawn a bunch of blackred_slash particles randomly within this 25 block radius sphere
- All entities within 25 blocks, every 5 ticks they have a 50% chance of getting hit, those that get hit will get 10 seconds of wither 5 potion effect
- Those that get hit get the blackred_impact particles on them and also get tossed around randomly within the 25 block radius sphere (using MovementHelper.AddVelocity)
- Every 2 ticks an explosion spawns at a random spot within this 25 block radius (with the explosion particles, the explosion damage and knockback, and breaking blocks (if the kimetsu block break gamerule for kimetsunoyaiba mod is enabled)
- At tick 56, do the Shockwave Blast Sequence

Ticks 67-82 (retraction)

- The user moves at 0.1x speed during this retraction phase
- R_2 starts at 25 on tick 67, and decreases to 1 on tick 82 (the radius of the sphere around the user's head)
- Large black dust particles spawn in this retracting sphere
- The 6 back tentacles and the 4 bone whips retract (their endpoints right on the edge of the sphere of radius R_2) as they slither back towards the user

Shockwave Blast Sequence:
First tick of the Shockwave Blast:

- Spawn 10 lightning sparks (yes the same ones from thunder breathing), they should each have a width of 0.8, and they should just be entirely white in color
- Play the minecraft:entity.warden.sonic_boom sound effect
- Play the thunder sound effect
- Remove the 3 flesh vines
- R starts at 1

The next 10 ticks of the Shockwave Blast:

- R increases every tick, reaching 40 by the 10th tick
- Every tick, spawn a sphere of radius R of flash particles around the user
- For each lightning spark, the start point is set to the user's center location (halfway between their head and location), and the end point is a random point along the edge of the sphere of radius R centered on the user (same sphere as the flash particles)
- For every living entity (other than the user) within R blocks of the user (use the distance squared trick to avoid the square root issue, comparing to R\*R, such that we don't have to use the slow power function), once they first enter the region, they become marked, meaning a new lightning spark will spawn on them (lasting 10 ticks), start point being the user's center position and end point being the living entity's center position (don't keep spawning more lightning sparks on the same living entity over and over again, if a living entity already has a lightning spark from this ability attached to them then just update the start and end positions every tick such that it tracks both entities)
- All living entities who get hit by this ability (they are within R of the user) will take a bunch of damage, and then immediately get slowness 20 for 5 seconds (no particles), and they will have the kimetsunoyaiba lightning particles on them for that duration, and they will also have the suffocation potion effect (from kimetsunoyaiba mod) for 5 seconds, and it will cancel any active abilities or breathing forms they have active (also any entity who has the suffocation effect shouldn't be able to use abilities, they cant use blood demon arts or breathing forms while they have the suffocation effect), and their velocities will also be instantly zeroed when they get hit by this ability

Tick 83 (cleanup)

- User's animations and guard state are cleared
- The 4 bone whips are removed
- User stops hovering (they are now affected by gravity again)
- Back tentacles are put back into a default state

## Demon King’s Blood Dance

_Tentacles, Bone Whips_

The ability selects up to the maximum of 15 mobs before automatically kneeling forward in a dance like motion whipping around multiple bone rips that tear apart the opponent’s bodies dealing high intensity damage in a mockery of a breathing style’s movement leaving the mob to bleed out at the end as they are unable to stop the user from walking off

Tick 0:

- The user plays a kneeling animation
- Biokineses item uses the biokineses_2 model (and the biokineses_2l model is rendered on the user's left hand)
- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)
- Mark up to 15 random targetable living entities (other than the user) within a 25 block radius
- If there are 6 or less entities, no bone whips will spawn (and it will just use the back tentacles)
- For every entity above 6, a bone whip will spawn (up to 9 bone whips, if there are a total of 15 selected entities)
- Each tentacle and bone whip is assigned a different target entity out of all of those selected entities
- The start position of every bone whip is attached to the user's torso

Ticks 0-15 (target acquisition):

- The user can't move during this chargeup phase, their speed reduced to 0 (but they are still affected by gravity, they can still fall)
- The tentacles and bone whips all shoot towards their respective targets (arcing upwards, local gravity aware), at a speed of 2 blocks per tick
- As soon as a tentacle or bone whip connects with their target entity, then that target entity will get stuck to that endpoint, always following the position of that endpoint
- If a tentacle / bone whip doesn't reach it's target within this 15 tick acquisition period, then it will lose the target (its target will be deselected) and it will just flail around randomly within the 25 block radius for the duration of the form

Ticks 16-76 (dance):

- All of the tentacles and bone whips flail around randomly, arcing randomly, within a 25 block radius of the user, dragging their targets along with them (that is if they were successful in reaching their target entities during the acquisition phase), and these target entities are also rotated and spun around randomly during this time, with 3 seconds of weakness 10 (no potion bubble particles), taking damage every 10 ticks and giving them the level 5 bleeding effect for 15 seconds

Ticks 77-87 (retraction):

- All the tentacles and bone whips dislodge from their targets (deselecting them, letting them go free) and retract towards the user

Tick 88 (cleanup):

- Clear the user's guard state and animation
- The user can move again
- Back tentacles are put back into a default state

## Abhorrent Galaxy

_Flesh vines, tentacles_

The user extends two large fleshy arms mottled with mouths, along with their back tentacles and leg tentacles, rotating them in a large spiral motion. The mouths on the arms are all sucking in air, pulling in nearby entities to be chewed by their hungry gnashing maws while the tentacles whip around and slice everything nearby

Tick 0

- Activate the six back tentacles (if they aren't active already, if they are active, then just take them out of whatever mode they are currently on, ending the biokineses climbing if that's happening, and place them on standby)
- Biokineses item uses the biokineses_1 model, and biokineses_1l is rendered in the left hand
- Place a small dark aura centered on the user's feet
- Place two large red circles auras centered on the user's feet, one spinning clockwise, the other spinning counter clockwise

Ticks 0-15 (chargeup)

- The dark aura grows larger
- The red circles auras grow smaller, and they spin faster and faster
- The user plays the right_arm_chargeup animation
- The six back tentacles spiral around the user in a 3 block radius sphere, curling inwards

Tick 15

- Create 4 flesh vines, their start location attached to one of the user's arms (two on the left arm, two on the right arm)
- Create 4 blood tornados (these ones don't climb and don't have gravity), these blood tornados should be evenly spaced across a 18 block radius centered on the user

Ticks 15-85

- X starts at 0 on tick 15, and it increases by 1 every tick (so it will be 70 by tick 85)
- The user plays the summon animation continuously
- The six back tentacles spiral around the user in a 25 block radius circle, each one curving
- The 4 flesh fines should spiral around the user like the arms of a spiral galaxy, curving appropriately, in a 25 block radius
- Along the path of each flesh vine, there should be flesh mouths facing random directions, each one should have inhale particles particles attached to them, and there should also be inhale particles attached to the ends of the flesh vines
- All living entities within 35 blocks of the user (and more than 5 blocks away from the user) are pulled towards the user, (if a living entity is within 3 blocks of being on the same Y level (or whatever the vertical coordinate is in the user's local gravity) of the user, then the living entity needs to be more than 20 blocks away to be pulled towards the user). This is to pull nearby entities into the 25 block radius disk
- Every 2 ticks, launch 4  blood lances outwards from the user horizontally (local gravity aware) at 90 degree increments, with an offset of `X * 3` degrees. Each blood lance should travel 25 blocks out before being removed.
- The blood tornados should follow the user, keep them in that circular pattern, they should be circling around the user in an 18 block radius
- Every 10 ticks each living entity within the disk has a 50% chance of taking damage from this form, and this damage should ignore the damage cooldown, and entities can also get damaged by the blood tornados and the blood lances
- If kimetsu block break gamerule is enabled, then every 10 ticks, up to 5 random blocks in the area of the disc will get tnt explosions, breaking blocks 

Ticks 86-96

- The back tentacles and the flesh vines retract back towards the user, taking a slithering path

Tick 97 (cleanup)

- The user's animations are cleared, the summon animation ends
- The back tentacles go back into a standard mode
- The flesh vines are removed

---

## Ignore everything past this line, this is WIP

---


## Titanic Globe, Ball of Genocide

The user grows many fleshy arms and appendages, wrapping around themself, forming a large ball of flesh and mouths. The user (in this ball form) rolls forward, crushing everything in its path. Entities who get hit by the ball will get grabbed by a mouth and stay stuck to the ball for the duration of the form, taking damage as they continuously get rolled over (and chewed by the mouth)
While in this form, the user can launch a flesh tentacle (on right click) and swing from it like a wrecking ball

Some random chargeup

- Create 4 brambles on the user's location, red and black in color, and 0.1 width
- The start location of all 4 brambles should be the user's location, and the end location should be 3 block's above the user (local gravity aware) and a random 1 block offset in any direction that is perpendicular to the upward vector (the upward vector in the local gravity)
- The start and end location of all 4 of those brambles should be recalculated every tick (with a new random end location each tick)
