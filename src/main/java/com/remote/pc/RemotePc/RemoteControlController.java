package com.remote.pc.RemotePc;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Robot;
import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

@RestController
@RequestMapping("/api/control")
@CrossOrigin(origins = "*")
public class RemoteControlController {

    
	private final Robot robot;

	public RemoteControlController() {
		this.robot = createRobot();
	}

	private Robot createRobot() {
		if (GraphicsEnvironment.isHeadless()) {
			return null;
		}
		try {
			return new Robot();
		} catch (AWTException exception) {
			return null;
		}
	}

	@org.springframework.web.bind.annotation.GetMapping("/status")
	public ResponseEntity<String> status() {
		return robot == null
				? ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Desktop control unavailable")
				: ResponseEntity.ok("Desktop control ready");
	}

	@PostMapping("/move")
	public ResponseEntity<Void> move(@RequestBody MouseMove move) {
		if (robot == null) return unavailable();
		var position = java.awt.MouseInfo.getPointerInfo().getLocation();
		robot.mouseMove(position.x + move.dx, position.y + move.dy);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/click")
	public ResponseEntity<Void> click(@RequestBody Click click) {
		if (robot == null) return unavailable();
		int button = "right".equalsIgnoreCase(click.button)
				? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
		robot.mousePress(button);
		robot.mouseRelease(button);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/scroll")
	public ResponseEntity<Void> scroll(@RequestBody Scroll scroll) {
		if (robot == null) return unavailable();
		robot.mouseWheel(Math.max(-10, Math.min(10, scroll.amount)));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/type")
	public ResponseEntity<Void> type(@RequestBody TypeText request) {
		if (robot == null) return unavailable();
		for (char character : request.text.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) {
				continue;
			}
			boolean shift = Character.isUpperCase(character) || "~!@#$%^&*()_+{}|:\\\"<>?".indexOf(character) >= 0;
			if (shift) {
				robot.keyPress(KeyEvent.VK_SHIFT);
			}
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
			if (shift) {
				robot.keyRelease(KeyEvent.VK_SHIFT);
			}
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/key")
	public ResponseEntity<Void> key(@RequestBody KeyRequest request) {
		if (robot == null) return unavailable();
		Integer keyCode = keyCodes().get(request.key.toLowerCase());
		if (keyCode == null) {
			return ResponseEntity.badRequest().build();
		}
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		return ResponseEntity.ok().build();
	}

	private ResponseEntity<Void> unavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
	}

	private Map<String, Integer> keyCodes() {
		return Map.ofEntries(
			Map.entry("enter", KeyEvent.VK_ENTER), Map.entry("backspace", KeyEvent.VK_BACK_SPACE),
			Map.entry("tab", KeyEvent.VK_TAB), Map.entry("escape", KeyEvent.VK_ESCAPE),
			Map.entry("space", KeyEvent.VK_SPACE), Map.entry("up", KeyEvent.VK_UP),
			Map.entry("down", KeyEvent.VK_DOWN), Map.entry("left", KeyEvent.VK_LEFT),
			Map.entry("right", KeyEvent.VK_RIGHT), Map.entry("volumeup", KeyEvent.VK_KP_UP)
			//Map.entry("volumedown", KeyEvent.VK_DOWN), Map.entry("mute", KeyEvent.VK_VOLUME_MUTE),
			//Map.entry("playpause", KeyEvent.VK_MEDIA_PLAY_PAUSE), Map.entry("home", KeyEvent.VK_HOME)
		);
	}

	public static class MouseMove {
		public int dx;
		public int dy;
	}

	public static class Click {
		public String button;
	}

	public static class Scroll {
		public int amount;
	}

	public static class TypeText {
		public String text;
	}

	public static class KeyRequest {
		public String key;
	}
}