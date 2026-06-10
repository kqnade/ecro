use std::sync::{Arc, Mutex};

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
pub extern "C" fn ecro_get_terminal_size(width: *mut u16, height: *mut u16) -> i32 {
    match crossterm::terminal::size() {
        Ok((w, h)) => {
            unsafe {
                *width = w;
                *height = h;
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

const EVENT_NONE: i32 = 0;
const EVENT_KEY: i32 = 1;
const EVENT_RESIZE: i32 = 2;
const EVENT_MOUSE: i32 = 3;

#[unsafe(no_mangle)]
pub extern "C" fn ecro_poll_event() -> *mut EcroEvent {
    use crossterm::event::{poll, read, Event, KeyCode, KeyModifiers};
    use std::time::Duration;

    match poll(Duration::from_millis(10)) {
        Ok(true) => {
            match read() {
                Ok(Event::Key(key_event)) => {
                    let code = match key_event.code {
                        KeyCode::Char(c) => c as i32,
                        KeyCode::Up => 1001,
                        KeyCode::Down => 1002,
                        KeyCode::Left => 1003,
                        KeyCode::Right => 1004,
                        KeyCode::Enter => 13,
                        KeyCode::Esc => 27,
                        KeyCode::Backspace => 127,
                        KeyCode::Tab => 9,
                        _ => 0,
                    };
                    
                    let modifiers = if key_event.modifiers.contains(KeyModifiers::CONTROL) {
                        1
                    } else if key_event.modifiers.contains(KeyModifiers::ALT) {
                        2
                    } else {
                        0
                    };
                    
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
        _ => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ecro_free_event(event: *mut EcroEvent) {
    if !event.is_null() {
        unsafe {
            let _ = Box::from_raw(event);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
        let mut width: u16 = 0;
        let mut height: u16 = 0;
        let result = ecro_get_terminal_size(&mut width, &mut height);
        assert_eq!(result, 0);
        assert!(width > 0);
        assert!(height > 0);
    }
}
