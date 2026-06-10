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

#[allow(dead_code)]
const EVENT_KEY: i32 = 1;
const EVENT_RESIZE: i32 = 2;

#[unsafe(no_mangle)]
pub extern "C" fn ecro_poll_event() -> *mut EcroEvent {
    use crossterm::event::{poll, read, Event, KeyCode, KeyModifiers};
    use std::time::Duration;

    match poll(Duration::from_millis(10)) {
        Ok(true) => {
            match read() {
                Ok(Event::Key(key_event)) => {
                    let (code, modifiers) = match key_event.code {
                        KeyCode::Char(c) => {
                            let key_code = if key_event.modifiers.contains(KeyModifiers::CONTROL) {
                                // Ctrl+key returns control code (1-26)
                                (c as u8 & 0x1f) as i32
                            } else {
                                c as i32
                            };
                            let mods = if key_event.modifiers.contains(KeyModifiers::ALT) {
                                2
                            } else {
                                0
                            };
                            (key_code, mods)
                        }
                        KeyCode::Up => (1001, 0),
                        KeyCode::Down => (1002, 0),
                        KeyCode::Left => (1003, 0),
                        KeyCode::Right => (1004, 0),
                        KeyCode::Enter => (13, 0),
                        KeyCode::Esc => (27, 0),
                        KeyCode::Backspace => (127, 0),
                        KeyCode::Tab => (9, 0),
                        KeyCode::Home => (1005, 0),
                        KeyCode::End => (1006, 0),
                        KeyCode::PageUp => (1007, 0),
                        KeyCode::PageDown => (1008, 0),
                        KeyCode::Insert => (1009, 0),
                        KeyCode::Delete => (1010, 0),
                        KeyCode::F(n) => (2000 + (n as i32), 0),
                        _ => (0, 0),
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
pub extern "C" fn ecro_read_event() -> *mut EcroEvent {
    use crossterm::event::{read, Event, KeyCode, KeyModifiers};

    match read() {
        Ok(Event::Key(key_event)) => {
            let (code, modifiers) = match key_event.code {
                KeyCode::Char(c) => {
                    let key_code = if key_event.modifiers.contains(KeyModifiers::CONTROL) {
                        (c as u8 & 0x1f) as i32
                    } else {
                        c as i32
                    };
                    let mods = if key_event.modifiers.contains(KeyModifiers::ALT) {
                        2
                    } else {
                        0
                    };
                    (key_code, mods)
                }
                KeyCode::Up => (1001, 0),
                KeyCode::Down => (1002, 0),
                KeyCode::Left => (1003, 0),
                KeyCode::Right => (1004, 0),
                KeyCode::Enter => (13, 0),
                KeyCode::Esc => (27, 0),
                KeyCode::Backspace => (127, 0),
                KeyCode::Tab => (9, 0),
                _ => (0, 0),
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
