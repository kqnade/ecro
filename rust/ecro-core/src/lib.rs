use std::sync::Mutex;

static TERMINAL_STATE: Mutex<TerminalState> = Mutex::new(TerminalState::new());

struct TerminalState {
    raw_mode: bool,
    alternate_screen: bool,
}

impl TerminalState {
    const fn new() -> Self {
        Self {
            raw_mode: false,
            alternate_screen: false,
        }
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_init() -> i32 {
    0
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_shutdown() -> i32 {
    let _ = ecro_disable_raw_mode();
    let _ = ecro_leave_alternate_screen();
    0
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_enable_raw_mode() -> i32 {
    match crossterm::terminal::enable_raw_mode() {
        Ok(_) => {
            if let Ok(mut state) = TERMINAL_STATE.lock() {
                state.raw_mode = true;
            }
            0
        }
        Err(_) => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_disable_raw_mode() -> i32 {
    match crossterm::terminal::disable_raw_mode() {
        Ok(_) => {
            if let Ok(mut state) = TERMINAL_STATE.lock() {
                state.raw_mode = false;
            }
            0
        }
        Err(_) => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_enter_alternate_screen() -> i32 {
    use crossterm::ExecutableCommand;
    use std::io::stdout;

    match stdout().execute(crossterm::terminal::EnterAlternateScreen) {
        Ok(_) => {
            if let Ok(mut state) = TERMINAL_STATE.lock() {
                state.alternate_screen = true;
            }
            0
        }
        Err(_) => -1,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_leave_alternate_screen() -> i32 {
    use crossterm::ExecutableCommand;
    use std::io::stdout;

    match stdout().execute(crossterm::terminal::LeaveAlternateScreen) {
        Ok(_) => {
            if let Ok(mut state) = TERMINAL_STATE.lock() {
                state.alternate_screen = false;
            }
            0
        }
        Err(_) => -1,
    }
}

#[unsafe(no_mangle)]
/// # Safety
///
/// `width` and `height` must be valid writable pointers to `i32` values.
pub unsafe extern "C" fn ecro_get_terminal_size(width: *mut i32, height: *mut i32) -> i32 {
    match crossterm::terminal::size() {
        Ok((w, h)) => {
            unsafe {
                *width = w as i32;
                *height = h as i32;
            }
            0
        }
        Err(_) => -1,
    }
}

#[repr(C)]
pub struct EcroEvent {
    pub event_type: i32,
    pub key_code: i32,
    pub modifiers: i32,
}

#[allow(dead_code)]
const EVENT_KEY: i32 = 1;
const EVENT_RESIZE: i32 = 2;

const MOD_CONTROL: i32 = 1;
const MOD_ALT: i32 = 2;
const MOD_SHIFT: i32 = 4;

fn encode_modifiers(modifiers: crossterm::event::KeyModifiers) -> i32 {
    let mut result = 0;
    if modifiers.contains(crossterm::event::KeyModifiers::CONTROL) {
        result |= MOD_CONTROL;
    }
    if modifiers.contains(crossterm::event::KeyModifiers::ALT) {
        result |= MOD_ALT;
    }
    if modifiers.contains(crossterm::event::KeyModifiers::SHIFT) {
        result |= MOD_SHIFT;
    }
    result
}

fn encode_key_event(key_event: crossterm::event::KeyEvent) -> (i32, i32) {
    use crossterm::event::KeyCode;

    let modifiers = encode_modifiers(key_event.modifiers);
    let code = match key_event.code {
        KeyCode::Char(c) => {
            if key_event
                .modifiers
                .contains(crossterm::event::KeyModifiers::CONTROL)
            {
                match c {
                    '/' => 31,
                    _ => (c as u8 & 0x1f) as i32,
                }
            } else {
                c as i32
            }
        }
        KeyCode::Up => 1001,
        KeyCode::Down => 1002,
        KeyCode::Left => 1003,
        KeyCode::Right => 1004,
        KeyCode::Enter => 13,
        KeyCode::Esc => 27,
        KeyCode::Backspace => 127,
        KeyCode::Tab => 9,
        KeyCode::Home => 1005,
        KeyCode::End => 1006,
        KeyCode::PageUp => 1007,
        KeyCode::PageDown => 1008,
        KeyCode::Insert => 1009,
        KeyCode::Delete => 1010,
        KeyCode::F(n) => 2000 + (n as i32),
        _ => 0,
    };

    (code, modifiers)
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_poll_event() -> *mut EcroEvent {
    use crossterm::event::{Event, poll, read};
    use std::time::Duration;

    match poll(Duration::from_millis(10)) {
        Ok(true) => match read() {
            Ok(Event::Key(key_event)) => {
                let (code, modifiers) = encode_key_event(key_event);

                let event = EcroEvent {
                    event_type: EVENT_KEY,
                    key_code: code,
                    modifiers,
                };

                Box::into_raw(Box::new(event))
            }
            Ok(Event::Resize(w, h)) => {
                let event = EcroEvent {
                    event_type: EVENT_RESIZE,
                    key_code: w as i32,
                    modifiers: h as i32,
                };

                Box::into_raw(Box::new(event))
            }
            _ => std::ptr::null_mut(),
        },
        _ => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_read_event() -> *mut EcroEvent {
    use crossterm::event::{Event, read};

    match read() {
        Ok(Event::Key(key_event)) => {
            let (code, modifiers) = encode_key_event(key_event);

            let event = EcroEvent {
                event_type: EVENT_KEY,
                key_code: code,
                modifiers,
            };

            Box::into_raw(Box::new(event))
        }
        Ok(Event::Resize(w, h)) => {
            let event = EcroEvent {
                event_type: EVENT_RESIZE,
                key_code: w as i32,
                modifiers: h as i32,
            };

            Box::into_raw(Box::new(event))
        }
        _ => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
/// # Safety
///
/// `event` must be either null or a pointer returned by `ecro_poll_event` or `ecro_read_event`.
pub unsafe extern "C" fn ecro_free_event(event: *mut EcroEvent) {
    if !event.is_null() {
        unsafe {
            let _ = Box::from_raw(event);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crossterm::event::{KeyCode, KeyEvent, KeyModifiers};

    #[test]
    fn test_ecro_init_returns_zero() {
        let result = ecro_init();
        assert_eq!(result, 0);
    }

    #[test]
    fn test_ecro_shutdown_returns_zero() {
        let result = ecro_shutdown();
        assert_eq!(result, 0);
    }

    #[test]
    fn test_terminal_size() {
        let mut width: i32 = 0;
        let mut height: i32 = 0;
        let result = unsafe { ecro_get_terminal_size(&mut width, &mut height) };
        assert_eq!(result, 0);
        assert!(width > 0);
        assert!(height > 0);
    }

    #[test]
    fn test_encode_control_slash() {
        let event = KeyEvent::new(KeyCode::Char('/'), KeyModifiers::CONTROL);
        let (code, modifiers) = encode_key_event(event);
        assert_eq!(code, 31);
        assert_eq!(modifiers, MOD_CONTROL);
    }

    #[test]
    fn test_encode_control_shift_z() {
        let event = KeyEvent::new(
            KeyCode::Char('Z'),
            KeyModifiers::CONTROL | KeyModifiers::SHIFT,
        );
        let (code, modifiers) = encode_key_event(event);
        assert_eq!(code, 26);
        assert_eq!(modifiers, MOD_CONTROL | MOD_SHIFT);
    }
}
