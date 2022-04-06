Engine_Sand : CroneEngine {

	var synth;
	var warp;
	var buffer;
	var posbus;
	var panbus;
	var ampbus;
	var ratebus;
	var voices;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		//var s = Server.default;
		//Add SynthDefs

		SynthDef(\play, {
			|out=0, buf, pan=0|
			Out.ar(out, Pan2.ar(PlayBuf.ar(1, buf, doneAction:2), pan));
		}).add;

		SynthDef(\warp, {
			|out=0, buf, pos=0, pan=0, amp=1, gate=1, rate=1|
			var env = EnvGen.kr(Env.asr, gate, doneAction: 2);
			Out.ar(out, Pan2.ar(Warp1.ar(1, buf, pos*SinOsc.kr(0.1).range(0.97,1.03), rate)*env, pan, amp));
		}).add;

		posbus = Bus.control(Server.default, 4);
		posbus.setn([0,0,0,0]);
		panbus = Bus.control(Server.default, 4);
		panbus.setn([0,0,0,0]);
		ampbus = Bus.control(Server.default, 4);
		ampbus.setn([1,1,1,1]);
		ratebus = Bus.control(Server.default, 4);
		ratebus.setn([1,1,1,1]);

		voices = Array.fill(4,nil);
		buffer = Array.fill(4,nil);

		Server.default.sync;

		this.addCommand("load_buffer", "s", {|msg|
			4.do({|n|
			buffer[n].free;
	buffer[n] = Buffer.read(Server.default, msg[1], action: {|b|(b.bufnum).postln;});
				
			});
			//buffer = buffer.collect({Buffer.read(Server.default, msg[1], action: {|b|(b.bufnum).postln;});});
			//msg.postln;
		});

		this.addCommand("play", "i", {|msg|
			synth = Synth.new(\play, [\out, msg[1], \buf, buffer.bufnum]);
		});

		this.addCommand("warpstart", "i", {|msg|
			voices.put(msg[1]-1, Synth.new(\warp, [\out, 0, \buf, buffer.at(msg[1]-1).bufnum, \pos, posbus.subBus(msg[1]-1).asMap, \pan, panbus.subBus(msg[1]-1).asMap, \amp, ampbus.subBus(msg[1]-1).asMap, \rate, ratebus.subBus(msg[1]-1).asMap, \gate, 1]));
			//voices.put(msg[1]-1, Synth.new(\default));
			("voice " ++ msg[1] ++ " started").postln;
			(msg[1]-1).postln;
			//msg.postln;
		});

		this.addCommand("pos", "if", {|msg|
			posbus.subBus(msg[1]-1).set(msg[2]);
			//(msg[1] + " pos " + msg[2]).postln;
		});

		this.addCommand("pan", "if", {|msg|
			panbus.subBus(msg[1]-1).set(msg[2]);
		});

		this.addCommand("amp", "if", {|msg|
			ampbus.subBus(msg[1]-1).set(msg[2]);
			//(msg[1] + " amp " + msg[2]).postln;
		});

		this.addCommand("rate", "if", {|msg|
			ratebus.subBus(msg[1]-1).set(msg[2]);
		});

		this.addCommand("warpend", "i", {|msg|
			voices.at(msg[1]-1).set(\gate, 0);
			("voice " ++ msg[1] ++ " ended").postln;
		});
  
    this.addCommand("getbus", "i", {|msg|
    posbus.getn;
    panbus.getn;
    ampbus.getn;
    ratebus.getn;
    });
	}

	free {
             // here you should free resources (e.g. Synths, Buffers &c)
// and stop processes (e.g. Routines, Tasks &c)
		synth.free;
		buffer.do({|n,i| buffer.at(i).free});
		warp.free;
		posbus.free;
		panbus.free;
		ampbus.free;
		ratebus.free;
		voices.do({|n,i| voices.at(i).free});

	}

}