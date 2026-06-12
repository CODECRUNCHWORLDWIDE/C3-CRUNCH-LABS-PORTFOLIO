// Exercise 2 — A controlled, validated add-habit form
//
// Goal: Build a multi-field controlled form in React Native with per-field
//       validation, errors that appear only after a field is "touched", a
//       submit button disabled until the form is valid, and keyboard handling
//       that never covers the inputs.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
//   1. Drop this file into your Expo app as `src/screens/AddHabitScreen.tsx`
//      and render it from a stack screen (or temporarily as your App root).
//   2. Fill in the bodies marked `// TODO`. Do NOT change the exported
//      component's props or the `Habit` / `HabitDraft` shapes — the parent
//      (and Exercise 3 / the mini-project) depend on them.
//   3. Run `npx tsc --noEmit` — zero errors, zero `any`.
//   4. Run the app and verify the behavior described under "EXPECTED BEHAVIOR".
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `npx tsc --noEmit`: 0 errors, no `any`.
//   [ ] Submit is disabled while the form is invalid.
//   [ ] A field's error shows only AFTER that field has been blurred (touched).
//   [ ] Submitting calls `onSubmit` with a fully-typed HabitDraft and resets.
//   [ ] The keyboard never covers the active input.
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

import { useMemo, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';

// ----------------------------------------------------------------------------
// Domain types  (do not change — the rest of the app depends on these)
// ----------------------------------------------------------------------------

export type Frequency = 'daily' | 'weekly';

/** What the form produces. The id/createdAt are assigned by the store/server. */
export type HabitDraft = {
  name: string;
  frequency: Frequency;
  /** Optional numeric target, e.g. "8" glasses of water. Empty = no target. */
  target: number | null;
};

export type AddHabitScreenProps = {
  /** Called with a validated draft when the user submits. */
  onSubmit: (draft: HabitDraft) => void;
  /** Called when the user cancels. */
  onCancel: () => void;
};

// ----------------------------------------------------------------------------
// Validation
// ----------------------------------------------------------------------------

type FieldName = 'name' | 'target';
type Errors = Partial<Record<FieldName, string>>;

/** Raw, still-string form values as the user types them. */
type FormValues = {
  name: string;
  frequency: Frequency;
  target: string; // kept as a string while editing; parsed on submit
};

/**
 * Validate the current form values. Rules:
 *   - name: required, trimmed length 1..60.
 *   - target: optional, but if present must be a positive integer.
 * Return an Errors object; empty object means "valid".
 *
 * You MUST return a fresh object computed only from `values` (a pure function).
 */
function validate(values: FormValues): Errors {
  // TODO: implement the rules above and return the Errors object.
  throw new Error('validate not implemented');
}

/** Parse the validated string values into a typed HabitDraft. */
function toDraft(values: FormValues): HabitDraft {
  // TODO: trim the name; parse target ('' -> null, otherwise Number(target)).
  throw new Error('toDraft not implemented');
}

// ----------------------------------------------------------------------------
// Component
// ----------------------------------------------------------------------------

const EMPTY: FormValues = { name: '', frequency: 'daily', target: '' };

export function AddHabitScreen({ onSubmit, onCancel }: AddHabitScreenProps) {
  const [values, setValues] = useState<FormValues>(EMPTY);
  const [touched, setTouched] = useState<Set<FieldName>>(new Set());

  // Recompute errors whenever values change. (useMemo keeps it cheap and pure.)
  const errors = useMemo(() => validate(values), [values]);
  const isValid = Object.keys(errors).length === 0;

  // Helper: update one field by name.
  function setField<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues((v) => ({ ...v, [key]: value }));
  }

  // Helper: mark a field as touched (call on blur).
  function markTouched(field: FieldName) {
    // TODO: add `field` to the `touched` set (immutably).
    throw new Error('markTouched not implemented');
  }

  // Show an error only if the field is touched AND has an error.
  function errorFor(field: FieldName): string | undefined {
    // TODO: return errors[field] only when touched.has(field), else undefined.
    throw new Error('errorFor not implemented');
  }

  function handleSubmit() {
    // TODO:
    //   1. If !isValid, mark every field touched (so all errors show) and return.
    //   2. Otherwise call onSubmit(toDraft(values)) and reset values + touched.
    throw new Error('handleSubmit not implemented');
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.container}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={styles.title}>New habit</Text>

        {/* Name -------------------------------------------------------------- */}
        <Text style={styles.label}>Name</Text>
        <TextInput
          style={[styles.input, errorFor('name') && styles.inputError]}
          value={values.name}
          onChangeText={(t) => setField('name', t)}
          onBlur={() => markTouched('name')}
          placeholder="e.g. Read 20 minutes"
          autoFocus
          returnKeyType="next"
        />
        {errorFor('name') ? <Text style={styles.error}>{errorFor('name')}</Text> : null}

        {/* Frequency --------------------------------------------------------- */}
        <Text style={styles.label}>Frequency</Text>
        <View style={styles.segment}>
          {(['daily', 'weekly'] as const).map((f) => (
            <TouchableOpacity
              key={f}
              style={[styles.segmentItem, values.frequency === f && styles.segmentItemActive]}
              onPress={() => setField('frequency', f)}
              accessibilityRole="button"
              accessibilityState={{ selected: values.frequency === f }}
            >
              <Text style={values.frequency === f ? styles.segmentTextActive : styles.segmentText}>
                {f}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Target (optional) ------------------------------------------------- */}
        <Text style={styles.label}>Daily target (optional)</Text>
        <TextInput
          style={[styles.input, errorFor('target') && styles.inputError]}
          value={values.target}
          onChangeText={(t) => setField('target', t)}
          onBlur={() => markTouched('target')}
          placeholder="e.g. 8"
          keyboardType="number-pad"
          returnKeyType="done"
        />
        {errorFor('target') ? <Text style={styles.error}>{errorFor('target')}</Text> : null}

        {/* Actions ----------------------------------------------------------- */}
        <View style={styles.actions}>
          <TouchableOpacity style={styles.cancel} onPress={onCancel}>
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.submit, !isValid && styles.submitDisabled]}
            onPress={handleSubmit}
            disabled={!isValid}
            accessibilityState={{ disabled: !isValid }}
          >
            <Text style={styles.submitText}>Add habit</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

// ----------------------------------------------------------------------------
// Styles
// ----------------------------------------------------------------------------

const styles = StyleSheet.create({
  flex: { flex: 1 },
  container: { padding: 24, gap: 8 },
  title: { fontSize: 24, fontWeight: '700', marginBottom: 12 },
  label: { fontSize: 14, fontWeight: '600', color: '#374151', marginTop: 12 },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  inputError: { borderColor: '#dc2626' },
  error: { color: '#dc2626', fontSize: 13, marginTop: 4 },
  segment: { flexDirection: 'row', gap: 8 },
  segmentItem: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#d1d5db',
    alignItems: 'center',
  },
  segmentItemActive: { backgroundColor: '#7c3aed', borderColor: '#7c3aed' },
  segmentText: { color: '#374151', textTransform: 'capitalize' },
  segmentTextActive: { color: '#fff', fontWeight: '700', textTransform: 'capitalize' },
  actions: { flexDirection: 'row', gap: 12, marginTop: 28 },
  cancel: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#d1d5db',
    alignItems: 'center',
  },
  cancelText: { color: '#374151', fontWeight: '600' },
  submit: {
    flex: 2,
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: '#7c3aed',
    alignItems: 'center',
  },
  submitDisabled: { backgroundColor: '#c4b5fd' },
  submitText: { color: '#fff', fontWeight: '700' },
});

// ----------------------------------------------------------------------------
// EXPECTED BEHAVIOR (verify on a simulator)
// ----------------------------------------------------------------------------
//
//   - On open, the name field is focused and the keyboard is up; the inputs
//     are NOT covered by the keyboard (scroll if needed).
//   - The "Add habit" button is dimmed/disabled until the name is valid.
//   - Type one character in name, then clear it and tap elsewhere: the
//     "Name is required." error appears (because name is now touched).
//   - Enter "abc" in the target field and blur it: "Target must be a positive
//     whole number." appears.
//   - Fill name = "Drink water", frequency = daily, target = 8, submit:
//     onSubmit is called with { name: "Drink water", frequency: "daily",
//     target: 8 } and the form resets to empty + untouched.
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// validate:
//   function validate(values: FormValues): Errors {
//     const errors: Errors = {};
//     const name = values.name.trim();
//     if (name.length === 0) errors.name = 'Name is required.';
//     else if (name.length > 60) errors.name = 'Keep it under 60 characters.';
//     if (values.target.trim().length > 0) {
//       const n = Number(values.target);
//       if (!Number.isInteger(n) || n <= 0)
//         errors.target = 'Target must be a positive whole number.';
//     }
//     return errors;
//   }
//
// toDraft:
//   function toDraft(values: FormValues): HabitDraft {
//     return {
//       name: values.name.trim(),
//       frequency: values.frequency,
//       target: values.target.trim() === '' ? null : Number(values.target),
//     };
//   }
//
// markTouched:
//   setTouched((prev) => new Set(prev).add(field));
//
// errorFor:
//   return touched.has(field) ? errors[field] : undefined;
//
// handleSubmit:
//   if (!isValid) { setTouched(new Set<FieldName>(['name', 'target'])); return; }
//   onSubmit(toDraft(values));
//   setValues(EMPTY);
//   setTouched(new Set());
//
// ----------------------------------------------------------------------------
