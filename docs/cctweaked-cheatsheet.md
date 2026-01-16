# CC: Tweaked Cheatsheet

Quick reference for ComputerCraft components and Lua programming.

---

## Components Overview

| Item | Description | Crafting |
|------|-------------|----------|
| **Computer** | Programmable terminal | Stone + Glass Pane + Redstone |
| **Advanced Computer** | Color display, more features | Gold + Glass Pane + Redstone |
| **Turtle** | Mobile robot, can mine/build | Iron + Chest + Computer |
| **Mining Turtle** | Turtle with pickaxe | Turtle + Diamond Pickaxe |
| **Advanced Turtle** | Color screen turtle | Gold + Chest + Advanced Computer |
| **Monitor** | External display | Stone + Glass Panes |
| **Advanced Monitor** | Color touchscreen | Gold + Glass Panes |
| **Disk Drive** | Read/write floppy disks | Stone + Redstone |
| **Floppy Disk** | Portable storage | Paper + Redstone |
| **Printer** | Print pages | Stone + Redstone + Dye + Paper |
| **Speaker** | Play sounds/music | Stone + Noteblock + Redstone |
| **Modem** | Wireless communication | Stone + Redstone |
| **Wired Modem** | Wired networking | Stone + Redstone (different recipe) |

---

## Basic Lua Syntax

### Variables
```lua
local name = "Steve"        -- string
local count = 42            -- number
local active = true         -- boolean
local items = {}            -- table (array/dictionary)
```

### Print Output
```lua
print("Hello, World!")
print("Count: " .. count)   -- string concatenation with ..
```

### Conditionals
```lua
if count > 10 then
    print("Many items")
elseif count > 0 then
    print("Some items")
else
    print("No items")
end
```

### Loops
```lua
-- For loop (count)
for i = 1, 10 do
    print(i)
end

-- For loop (step)
for i = 10, 1, -1 do
    print(i)  -- countdown
end

-- While loop
while count > 0 do
    count = count - 1
end

-- Repeat until
repeat
    count = count + 1
until count >= 10

-- Iterate table
for key, value in pairs(myTable) do
    print(key, value)
end
```

### Functions
```lua
local function greet(name)
    return "Hello, " .. name
end

print(greet("Steve"))

-- Multiple returns
local function getPosition()
    return 10, 20, 30
end

local x, y, z = getPosition()
```

### Tables (Arrays & Dictionaries)
```lua
-- Array (indexed by number)
local fruits = {"apple", "banana", "cherry"}
print(fruits[1])  -- "apple" (Lua arrays start at 1!)

-- Dictionary
local player = {
    name = "Steve",
    health = 20,
    inventory = {}
}
print(player.name)
print(player["health"])

-- Add to table
table.insert(fruits, "date")
player.armor = 10
```

---

## Computer Basics

### Terminal Commands
```
lua              -- Interactive Lua prompt
edit <filename>  -- Open text editor
run <filename>   -- Run a program
ls               -- List files
cd <dir>         -- Change directory
mkdir <dir>      -- Create directory
rm <file>        -- Delete file
cp <src> <dest>  -- Copy file
mv <src> <dest>  -- Move/rename file
label set <name> -- Name the computer
id               -- Show computer ID
reboot           -- Restart computer
shutdown         -- Turn off
```

### Your First Program

1. Type `edit hello` to create a new file
2. Write:
```lua
print("Hello from CC: Tweaked!")
for i = 1, 5 do
    print("Count: " .. i)
    sleep(1)
end
print("Done!")
```
3. Press `Ctrl` to open menu, select **Save**
4. Press `Ctrl` again, select **Exit**
5. Type `hello` to run it

### Startup Program
Create a file named `startup` or `startup.lua` - it runs automatically when the computer boots.

---

## Turtle Programming

### Movement
```lua
turtle.forward()      -- Move forward
turtle.back()         -- Move backward
turtle.up()           -- Move up
turtle.down()         -- Move down
turtle.turnLeft()     -- Turn left
turtle.turnRight()    -- Turn right
```

### Digging & Placing
```lua
turtle.dig()          -- Dig block in front
turtle.digUp()        -- Dig block above
turtle.digDown()      -- Dig block below
turtle.place()        -- Place block in front
turtle.placeUp()      -- Place block above
turtle.placeDown()    -- Place block below
```

### Inventory
```lua
turtle.select(1)           -- Select slot 1-16
turtle.getItemCount()      -- Items in current slot
turtle.getItemDetail()     -- Info about current slot
turtle.drop()              -- Drop items forward
turtle.dropUp()            -- Drop items up
turtle.dropDown()          -- Drop items down
turtle.suck()              -- Pick up items in front
turtle.suckUp()            -- Pick up items above
turtle.suckDown()          -- Pick up items below
turtle.transferTo(slot, count)  -- Move items between slots
```

### Fuel
```lua
turtle.getFuelLevel()      -- Current fuel
turtle.getFuelLimit()      -- Max fuel
turtle.refuel()            -- Consume fuel from current slot
turtle.refuel(10)          -- Consume up to 10 items as fuel
```

### Detection
```lua
turtle.detect()            -- Block in front?
turtle.detectUp()          -- Block above?
turtle.detectDown()        -- Block below?
turtle.inspect()           -- Get block info in front
turtle.inspectUp()         -- Get block info above
turtle.inspectDown()       -- Get block info below
turtle.compare()           -- Compare to selected slot
```

### Attack & Interact
```lua
turtle.attack()            -- Attack entity in front
turtle.attackUp()          -- Attack entity above
turtle.attackDown()        -- Attack entity below
```

### Example: Simple Mining
```lua
-- Mine a 3x3 tunnel, 10 blocks deep
local function mineForward()
    while turtle.detect() do
        turtle.dig()
        sleep(0.5)
    end
    turtle.forward()
end

for i = 1, 10 do
    -- Mine 3x3
    mineForward()
    turtle.digUp()
    turtle.digDown()

    turtle.turnLeft()
    turtle.dig()
    turtle.turnRight()
    turtle.turnRight()
    turtle.dig()
    turtle.turnLeft()
end
print("Tunnel complete!")
```

### Example: Tree Farmer
```lua
local function chopTree()
    -- Chop up
    while turtle.detect() do
        turtle.dig()
        turtle.digUp()
        turtle.up()
    end
    -- Come back down
    while not turtle.detectDown() do
        turtle.down()
    end
end

while true do
    if turtle.detect() then
        local success, data = turtle.inspect()
        if data and data.name:find("log") then
            chopTree()
        end
    end
    sleep(60)  -- Check every minute
end
```

---

## Peripherals

### Finding Peripherals
```lua
peripheral.getNames()          -- List all attached
peripheral.getType("left")     -- Type of peripheral on left
peripheral.isPresent("right")  -- Is something on right?
peripheral.wrap("left")        -- Get peripheral object
```

### Monitor
```lua
local mon = peripheral.wrap("top")  -- or "left", "right", etc.

mon.clear()                    -- Clear screen
mon.setCursorPos(1, 1)         -- Set cursor position
mon.write("Hello!")            -- Write text
mon.setTextScale(2)            -- Scale 0.5 to 5
mon.setTextColor(colors.red)   -- Set text color
mon.setBackgroundColor(colors.black)
mon.getSize()                  -- Returns width, height

-- Redirect all output to monitor
term.redirect(mon)
print("This prints on monitor!")
term.restore()  -- Switch back to computer
```

### Disk Drive
```lua
local drive = peripheral.wrap("bottom")

disk.isPresent("bottom")       -- Disk inserted?
disk.getLabel("bottom")        -- Get disk label
disk.setLabel("bottom", "My Disk")
disk.getMountPath("bottom")    -- Returns path like "disk"

-- Access files on disk
local file = fs.open("disk/myfile.txt", "w")
file.write("Hello from disk!")
file.close()
```

### Speaker
```lua
local speaker = peripheral.wrap("right")

speaker.playNote("harp", 1, 12)    -- instrument, volume, pitch
speaker.playSound("entity.pig.ambient")
speaker.playSound("minecraft:block.note_block.harp")
```

### Modem (Wireless)
```lua
local modem = peripheral.wrap("back")

modem.open(1)                  -- Open channel 1
modem.transmit(1, 1, "Hello")  -- Send on channel 1
modem.close(1)                 -- Close channel

-- Receive messages
modem.open(1)
while true do
    local event, side, channel, replyChannel, message = os.pullEvent("modem_message")
    print("Received: " .. tostring(message))
end
```

### Rednet (Simplified Networking)
```lua
rednet.open("back")            -- Open modem on back

-- Send
rednet.send(5, "Hello")        -- Send to computer ID 5
rednet.broadcast("Hello all")  -- Send to everyone

-- Receive
local id, message = rednet.receive()
print("From " .. id .. ": " .. message)

-- With timeout
local id, message = rednet.receive(5)  -- Wait 5 seconds

rednet.close()
```

---

## Useful APIs

### Sleep & Time
```lua
sleep(2)                       -- Wait 2 seconds
os.time()                      -- Minecraft time (0-24)
os.day()                       -- Minecraft day count
os.epoch("utc")                -- Real timestamp (ms)
```

### Events
```lua
-- Wait for any event
local event, param1, param2 = os.pullEvent()

-- Wait for specific event
local event, key = os.pullEvent("key")

-- Common events:
-- "key" - key pressed
-- "char" - character typed
-- "mouse_click" - mouse button, x, y
-- "timer" - timer fired
-- "redstone" - redstone signal changed
-- "modem_message" - network message
```

### Timers
```lua
local timerID = os.startTimer(5)  -- Fire in 5 seconds

while true do
    local event, id = os.pullEvent("timer")
    if id == timerID then
        print("Timer fired!")
        break
    end
end
```

### Files
```lua
-- Write file
local file = fs.open("myfile.txt", "w")
file.write("Line 1\n")
file.writeLine("Line 2")
file.close()

-- Read file
local file = fs.open("myfile.txt", "r")
local contents = file.readAll()
file.close()
print(contents)

-- Check if exists
if fs.exists("myfile.txt") then
    print("File exists!")
end

-- List directory
local files = fs.list("/")
for _, name in ipairs(files) do
    print(name)
end
```

### HTTP Requests
```lua
-- GET request
local response = http.get("https://example.com/api")
if response then
    print(response.readAll())
    response.close()
end

-- POST request
local response = http.post("https://example.com/api", "data=value")
```

### JSON
```lua
local data = {name = "Steve", score = 100}
local json = textutils.serializeJSON(data)
print(json)  -- {"name":"Steve","score":100}

local parsed = textutils.unserializeJSON(json)
print(parsed.name)  -- Steve
```

### Colors (Advanced Computers/Monitors)
```lua
colors.white      colors.orange     colors.magenta
colors.lightBlue  colors.yellow     colors.lime
colors.pink       colors.gray       colors.lightGray
colors.cyan       colors.purple     colors.blue
colors.brown      colors.green      colors.red
colors.black

term.setTextColor(colors.red)
term.setBackgroundColor(colors.white)
```

---

## Common Patterns

### Main Loop
```lua
while true do
    -- Do something
    print("Working...")
    sleep(1)
end
```

### Event Loop
```lua
while true do
    local event, p1, p2, p3 = os.pullEvent()

    if event == "key" then
        if p1 == keys.q then
            break  -- Quit on Q
        end
    elseif event == "timer" then
        print("Timer!")
    end
end
```

### Error Handling
```lua
local success, error = pcall(function()
    -- Risky code here
    turtle.forward()
end)

if not success then
    print("Error: " .. error)
end
```

### Parallel Tasks
```lua
parallel.waitForAny(
    function()
        while true do
            turtle.forward()
            sleep(1)
        end
    end,
    function()
        sleep(10)
        print("Stopping after 10 seconds")
    end
)
```

---

## Quick Reference

| Task | Code |
|------|------|
| Wait 5 seconds | `sleep(5)` |
| Move turtle forward | `turtle.forward()` |
| Dig block | `turtle.dig()` |
| Check fuel | `turtle.getFuelLevel()` |
| Refuel | `turtle.refuel()` |
| Print to monitor | `peripheral.wrap("top").write("Hi")` |
| Send wireless message | `rednet.broadcast("msg")` |
| Read file | `fs.open("file","r").readAll()` |
| Run program | `shell.run("program")` |

---

## Resources

- **Official Docs:** https://tweaked.cc/
- **Wiki:** https://www.computercraft.info/wiki/
- **Programs:** https://pastebin.com (use `pastebin get <code> <filename>`)
