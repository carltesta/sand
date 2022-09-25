-- Sand
-- Prototype Granulator
--
-- designed for 
-- the sensel morph
fileselect = require('fileselect')
util = require 'util'
engine.name = "Sand"

-- table for msb and lsb for all 4 voices plus converted values 9 slots per voice i.e. -- xmsb = values[1][1] -- xlsb = values[1][2]
values = { {0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0} }

toggle = 0

function init()

--params:add_separator("audio settings")

for chan=1,4 do
params:add_separator()
params:add_option("mode_select"..chan,chan..": mode select",{"Single File","Sequence","Random"}, 1)
params:set_action("mode_select"..chan, function(x) modeselect(x,chan) end)
params:add_file("audio_file"..chan,chan..": audio file",audio.path)
params:set_action("audio_file"..chan, function(x) engine.load_buffer(chan, params:get("audio_file"..chan)) end)
params:add_file("folder_select"..chan,chan..": folder select", _path.audio)
params:set_action("folder_select"..chan, function(x) print("you did something") end)
params:add_control("position"..chan,chan..": position",controlspec.new(0,1,'lin',0.0001,0.5))
params:set_action("position"..chan, function(x) engine.pos(chan,x) end)
params:add_control("rate"..chan,chan..": rate",controlspec.RATE)
params:set_action("rate"..chan, function(x) engine.rate(chan,x) end)
params:add_control("pan"..chan,chan..": pan",controlspec.PAN)
params:set_action("pan"..chan, function(x) engine.pan(chan,x) end)
params:add_control("amp"..chan,chan..": amp",controlspec.AMP)
params:set_action("amp"..chan, function(x) engine.amp(chan,x) end)
params:add_control("mix"..chan,chan..": filter mix",controlspec.UNIPOLAR)
params:set_action("mix"..chan, function(x) engine.mix(chan,x) end)
params:add_control("ffreq"..chan,chan..": filter freq",controlspec.FREQ)
params:set_action("ffreq"..chan, function(x) engine.ffreq(chan,x) end)
params:add_control("res"..chan,chan..": filter res",controlspec.new(0, 4, 'lin', 0.01, 0.5))
params:set_action("res"..chan, function(x) engine.res(chan,x) end)
params:hide("folder_select"..chan)
_menu.rebuild_params()
end



params:add_separator("MIDI MPE settings")
--params:add_option("midi target", "midi target",midi_device_names,1)
--params:set_action("midi target", function(x) target = x end)
params:add_number("X_MSB","X MSB: ",1,128,72)
params:add_number("X_LSB","X LSB: ",1,128,104)
params:add_number("Y_MSB","Y MSB: ",1,128,74)
params:add_number("Y_LSB","Y LSB: ",1,128,106)
params:add_number("Z_MSB","Z MSB: ",1,128,76)
params:add_number("Z_LSB","Z LSB: ",1,128,108)

t = metro.init()
  t.count = 0
  t.time = 1/60
  t.event = function(stage)
    redraw()
  end
  t:start()
--params:bang()
end

  m = midi.connect()
  m.event = function(data) 
  local d = midi.to_msg(data)
  if d.ch < 5 then
  if d.type == "note_on" then engine.grainstart(d.ch) end
  if d.type == "note_off" then engine.warpend(d.ch) end
  if d.cc == params:get("X_MSB") then values[d.ch][1] = d.val end
  if d.cc == params:get("X_LSB") then values[d.ch][2] = d.val end
  if d.cc == params:get("Y_MSB") then values[d.ch][3] = d.val end
  if d.cc == params:get("Y_LSB") then values[d.ch][4] = d.val end
  if d.cc == params:get("Z_MSB") then values[d.ch][5] = d.val end
  if d.cc == params:get("Z_LSB") then values[d.ch][6] = d.val end
  values[d.ch][7] = convert14bit(values[d.ch][1],values[d.ch][2])
  values[d.ch][8] = convert14bit(values[d.ch][3],values[d.ch][4])
  values[d.ch][9] = convert14bit(values[d.ch][5],values[d.ch][6])
  params:set("position"..d.ch, values[d.ch][7])
  params:set("ffreq"..d.ch,util.linlin(0,1,50,20000,values[d.ch][8]))
  params:set("amp"..d.ch, values[d.ch][9])
  --print(xvalue)
  end
  --tab.print(d)
end

--function load_buffer(chan,)
  --engine.load_buffer(chan, params:get("audio_file"..chan))
  --end

function redraw()
screen.clear()
  screen.aa(1)
  screen.move(0, 8)
  screen.font_size(8)
  screen.level(10)
  screen.text("SAND")
  screen.move(0,24)
  if params:get("audio_file"..1) == "-" then
  screen.text("select audio in params menu")
  else
    --screen.text(params:get("audio_file"))
  end
  for i=1,4 do
  screen.stroke()
  screen.circle(values[i][7]*128,64-(values[i][8]*64),values[i][9]*32)
  end
  for i=1,4 do
    screen.stroke()
    screen.move(20*i,40)
    screen.text(string.format("%.2f", params:get("rate"..i)))
  end
  for i=1,4 do
    screen.stroke()
    screen.move(20*i,50)
    screen.text(string.format("%.2f", params:get("res"..i)))
  end
  screen.update()
end

function enc(n,d)
  if n == 2 then
    --params:delta("position", d)
end
end

function key(n,z)
  if n == 2 and z == 1 then
    engine.play(0)
    redraw()
  end
end

function convert14bit(msb,lsb)
  -- function to convert 14 bit midi to floats between 0 and 1
    local val = (bit32.lshift(msb,7) + lsb)/16383
    return val
end

function modeselect(mode,chan)
  -- function to show and hide params based on mode
  if mode == 1 then
  print("mode 1 selected")
  params:show("audio_file"..chan)
  params:hide("folder_select"..chan)
  _menu.rebuild_params()
  else
  if mode == 2 then
    print("mode 2 selected")
    params:hide("audio_file"..chan)
    params:show("folder_select"..chan)
    _menu.rebuild_params()
  else
    print("mode 3 selected")
    params:hide("audio_file"..chan)
    params:show("folder_select"..chan)
    _menu.rebuild_params()
  end
  end
end
