Engine_Sand : CroneEngine {

	var synth;
	var warp;
	var buffer;
	var posbus;
	var panbus;
	var ampbus;
	var ratebus;
	var mixbus;
	var ffreqbus;
	var resbus;
	var voices;
	var env1, env2, env3, envA, envB, envC;

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
			|out=0, buf, pos=0, pan=0, amp=1, gate=1, rate=1, ffreq=10000, res=0.5, mix=0|
			var env = EnvGen.kr(Env.asr, gate, doneAction: 2);
			var grain = Pan2.ar(Warp1.ar(1, buf, pos*SinOsc.kr(0.1).range(0.97,1.03), rate)*env, pan, amp);
			var filter = MoogFF.ar(grain, ffreq, res);
			Out.ar(out, SelectX.ar(mix,[grain,filter]));
		}).add;
		
		SynthDef(\buffergrain, {
		| bufnum, out=0,trig=1, rate = 1, pos = 0.5, gSize = 0.5, amp=1, pan=0, maxGrain=512, envbuf = -1,ffreq=10000,res=0.5|
		var grain, filter;
		grain = GrainBuf.ar(2, Impulse.kr(trig), gSize, bufnum, rate, pos, 2, pan, envbuf, maxGrain, amp);
		filter = MoogFF.ar(grain, ffreq, res);
    Out.ar(out,filter);
    }).add;


		posbus = Bus.control(Server.default, 4);
		posbus.setn([0,0,0,0]);
		panbus = Bus.control(Server.default, 4);
		panbus.setn([0,0,0,0]);
		ampbus = Bus.control(Server.default, 4);
		ampbus.setn([1,1,1,1]);
		ratebus = Bus.control(Server.default, 4);
		ratebus.setn([1,1,1,1]);
		mixbus = Bus.control(Server.default, 4);
		mixbus.setn([0,0,0,0]);
		ffreqbus = Bus.control(Server.default, 4);
		ffreqbus.setn([10000,10000,10000,10000]);
		resbus = Bus.control(Server.default, 4);
		resbus.setn([0.5,0.5,0.5,0.5]);
		
		voices = Array.fill(4,nil);
		buffer = Array.fill(4,nil);
		
		env1 = Env.perc(0.001, 1, 1, -4);
    envA = Buffer.sendCollection(Server.default, env1.discretize, 1);
    env2 = Env.asr(0.25, 1, 1, -4);
    envB = Buffer.sendCollection(Server.default, env2.discretize, 1);
    env3 = Env.asr(0.7, 1, 1, -4);
    envC = Buffer.sendCollection(Server.default, env3.discretize, 1);

		Server.default.sync;

		this.addCommand("load_buffer", "is", {|msg|
			buffer[msg[1]-1].free;
	    buffer[msg[1]-1] = Buffer.read(Server.default, msg[2], action: {|b|(b.bufnum).postln;});
				
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
		
		this.addCommand("grainstart", "i", {|msg|
			voices.put(msg[1]-1, 
			Synth.new(\warp, 
			[\out, 0, 
			\buf, buffer.at(msg[1]-1).bufnum,
			\trig, ([0.5,1,2,3,4]*3).choose,
			\pos, posbus.subBus(msg[1]-1).asMap, 
			\pan, panbus.subBus(msg[1]-1).asMap, 
			\amp, ampbus.subBus(msg[1]-1).asMap, 
			\rate, ratebus.subBus(msg[1]-1).asMap,
			\envbuf, [envA, envB].choose,
			\mix, mixbus.subBus(msg[1]-1).asMap,
			\ffreq, ffreqbus.subBus(msg[1]-1).asMap,
			\res, resbus.subBus(msg[1]-1).asMap,
			\gate, 1]));
			//voices.put(msg[1]-1, Synth.new(\default));
			("voice " ++ msg[1] ++ " started").postln;
			(msg[1]-1).postln;
			//msg.postln;
		});
		
				//\gSize, xys[2]/2,
				//\rate, [0.25, 0.5, 1, 1.6, 2].choose,

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
		
		this.addCommand("mix", "if", {|msg|
		mixbus.subBus(msg[1]-1).set(msg[2]);
		});
		
		this.addCommand("ffreq", "if", {|msg|
		ffreqbus.subBus(msg[1]-1).set(msg[2]);
		});
		
		this.addCommand("res", "if", {|msg|
		resbus.subBus(msg[1]-1).set(msg[2]);
		});
  
    this.addCommand("getbus", "i", {|msg|
    posbus.getn;
    panbus.getn;
    ampbus.getn;
    ratebus.getn;
    mixbus.getn;
    ffreqbus.getn;
    resbus.getn;
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
		mixbus.free;
		ffreqbus.free;
		resbus.free;
		voices.do({|n,i| voices.at(i).free});

	}

}
