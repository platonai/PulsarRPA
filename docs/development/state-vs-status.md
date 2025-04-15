# State vs Status

In computer science, **status** and **state** are related but distinct concepts. Their differences are primarily reflected in the following aspects:

---

### **1. Definition**
- **Status**:
    - Typically refers to the **current condition** or **result** of a system, process, or operation.
    - Often describes a **transient**, **discrete** value, such as an HTTP status code (e.g., 200, 404) or a process exit status.
    - Example: An HTTP request returning a `200 OK` status code indicates success.

- **State**:
    - Refers to the **complete condition** of a system, object, or component, often representing a **persistent**, **internal** set of attributes.
    - Commonly used to describe a **continuous**, **internal** condition, such as the state in a finite state machine (FSM).
    - Example: A thread's state might be `RUNNING`, `WAITING`, or `TERMINATED`.

---

### **2. Usage Context**
- **Status**:
    - Used to describe **externally visible** results or conditions.
    - Commonly seen in API responses, process exit codes, task execution results, etc.
    - Examples:
        - HTTP status codes: `200 OK`, `404 Not Found`.
        - Process exit status: `0` for success, non-zero for errors.

- **State**:
    - Used to describe **internal conditions** or **context**.
    - Commonly seen in state machines, object lifecycles, system internal states, etc.
    - Examples:
        - Thread states: `NEW`, `RUNNABLE`, `BLOCKED`.
        - Finite State Machine (FSM) states: `IDLE`, `PROCESSING`, `COMPLETED`.

---

### **3. Temporal Dimension**
- **Status**:
    - Typically **transient**, representing a condition at a specific point in time.
    - Example: An HTTP status code returned after a request completes.

- **State**:
    - Typically **persistent**, representing a condition over a period of time.
    - Example: The lifecycle of a thread from start to finish.

---

### **4. Granularity**
- **Status**:
    - Usually a **single**, **discrete** value.
    - Example: A task's status might be `SUCCESS` or `FAILURE`.

- **State**:
    - Usually **composite**, involving multiple attributes or variables.
    - Example: An object's state might include multiple fields (e.g., `isActive`, `isRunning`).

---

### **5. Examples**
#### **Status Examples**:
- HTTP status codes:
    - `200 OK`: Request succeeded.
    - `500 Internal Server Error`: Server encountered an error.
- Process exit status:
    - `0`: Success.
    - `1`: General error.

#### **State Examples**:
- Thread states:
    - `NEW`: Thread created but not started.
    - `RUNNABLE`: Thread is running or ready to run.
    - `TERMINATED`: Thread has terminated.
- Finite State Machine (FSM):
    - `IDLE`: System is idle.
    - `PROCESSING`: System is processing a task.
    - `COMPLETED`: Task is completed.

---

### **Summary**
| Feature         | Status                          | State                          |
|-----------------|---------------------------------|--------------------------------|
| **Definition**  | Transient condition or result   | Persistent internal condition  |
| **Usage**       | Externally visible results      | Internal conditions            |
| **Temporal**    | Transient                      | Persistent                     |
| **Granularity** | Single, discrete value         | Composite, multiple attributes |
