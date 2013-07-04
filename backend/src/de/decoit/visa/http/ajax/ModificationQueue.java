/*
 *  Copyright (C) 2013, DECOIT GmbH
 *
 *	This file is part of VISA Topology-Editor.
 *
 *	VISA Topology-Editor is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by the
 *	Free Software Foundation, either version 3 of the License, or (at your option)
 *	any later version.
 *
 *	VISA Topology-Editor is distributed in the hope that it will be useful, but
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *	more details.
 *
 *	You should have received a copy of the GNU General Public License along with
 *	VISA Topology-Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package de.decoit.visa.http.ajax;

import java.util.ArrayList;
import de.decoit.visa.enums.ModificationTarget;
import de.decoit.visa.enums.ModificationTargetAttribute;


/**
 * This class is used to queue modifications received from the frontend. The
 * stored modifications can then be processed by a handler or discarded if the
 * user aborts the operation.
 *
 * @author Thomas Rix
 */
public class ModificationQueue {
	private String queueID;
	private ArrayList<Modification> mods;


	/**
	 * Construct a new ModificationQueue object. It is identified by the
	 * provided ID string.
	 *
	 * @param pQueueID ID for this queue
	 */
	ModificationQueue(String pQueueID) {
		if(pQueueID.isEmpty()) {
			throw new IllegalArgumentException("Empty queue ID string provided");
		}
		queueID = pQueueID;

		mods = new ArrayList<>();
	}


	/**
	 * Add a new {@link Modification} object to this queue. The target attribute
	 * must fit into the combinations listed in the documentation of this class.
	 *
	 * @param pTarget Target type
	 * @param pTargetID Local name of the target, cannot be null or empty
	 * @param pAttrib Target attribute type
	 * @param pValue New value for the attribute type
	 */
	public void addModification(ModificationTarget pTarget, String pTargetID, ModificationTargetAttribute pAttrib, String pValue) {
		mods.add(new Modification(pTarget, pTargetID, pAttrib, pValue));
	}


	/**
	 * Return a HashSet of all modifications stored in this queue
	 *
	 * @return A HashSet of all modifications
	 */
	public ArrayList<Modification> getModifications() {
		return mods;
	}


	/**
	 * Remove all queued modifications from this queue
	 */
	public void discard() {
		mods.clear();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queueID == null) ? 0 : queueID.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof ModificationQueue) {
			ModificationQueue mq = (ModificationQueue) obj;

			return (mq.queueID.equals(queueID));
		}
		else {
			return false;
		}
	}


	/**
	 * This defines a single modification which will be done on a specific
	 * modification target. Possible targets are defined in the
	 * {@link de.decoit.visa.enums.ModificationTarget ModificationTarget}
	 * enumeration class, modified attributes of these targets in the
	 * {@link de.decoit.visa.enums.ModificationTargetAttribute
	 * ModificationTargetAttribute} enumeration class.<br>
	 * <br>
	 * This class checks for valid combinations of targets and target
	 * attributes. The allowed combinations are printed below, all other
	 * combinations will be rejected with an IllegalArgumentException.<br>
	 * <br>
	 * <strong>Target:</strong> COMPONENT<br>
	 * <strong>Attributes:</strong> COMPONENT_NAME<br>
	 * <br>
	 * <strong>Target:</strong> INTERFACE<br>
	 * <strong>Attributes:</strong> INTERFACE_NETWORK, INTERFACE_ORIENTATION
	 *
	 * @author Thomas Rix
	 */
	public class Modification {
		private ModificationTarget target;
		private String targetID;
		private ModificationTargetAttribute attrib;
		private String attribValue;


		/**
		 * Create a new Modification object with the specified target
		 * attribute and attribute value. The target is defined by the target
		 * type (value of {@link de.decoit.visa.enums.ModificationTarget
		 * ModificationTarget}) and the local name of the target object.
		 *
		 * @param pTarget Target type
		 * @param pTargetID Local name of the target, cannot be null or empty
		 * @param pAttrib Attribute type, must match the possible attributes of
		 *            the target type as defined in the documentation of the
		 *            Modification class
		 * @param pValue New value for the attribute, cannot be null or empty
		 */
		private Modification(ModificationTarget pTarget, String pTargetID, ModificationTargetAttribute pAttrib, String pValue) {
			if(pTargetID.isEmpty()) {
				throw new IllegalArgumentException("Empty target ID string provided");
			}

			if(pValue.isEmpty()) {
				throw new IllegalArgumentException("Empty attribute value string provided");
			}

			target = pTarget;
			targetID = pTargetID;

			if(target == ModificationTarget.COMPONENT) {
				switch(pAttrib) {
					case COMPONENT_NAME:
						attrib = pAttrib;
						attribValue = pValue;
						break;
					default:
						throw new IllegalArgumentException("Illegal target attribute for COMPONENT target specified");
				}
			}
			else if(target == ModificationTarget.INTERFACE) {
				switch(pAttrib) {
					case INTERFACE_NETWORK:
					case INTERFACE_ORIENTATION:
						attrib = pAttrib;
						attribValue = pValue;
						break;
					default:
						throw new IllegalArgumentException("Illegal target attribute for INTERFACE target specified");
				}
			}
			else {
				throw new IllegalArgumentException("Unknown modifcation target (possibly NULL) provided");
			}
		}


		/**
		 * Return the target type of this modification
		 *
		 * @return Target type of this modification
		 */
		public ModificationTarget getTargetType() {
			return target;
		}


		/**
		 * Return the local name of the target object
		 *
		 * @return RDF local name of the target object
		 */
		public String getTargetLocalName() {
			return targetID;
		}


		/**
		 * Return the target attribute of the target object
		 *
		 * @return Target attribute of the target object
		 */
		public ModificationTargetAttribute getTargetAttribute() {
			return attrib;
		}


		/**
		 * Return the new attribute value
		 *
		 * @return New attribute value
		 */
		public String getTargetAttributeValue() {
			return attribValue;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((attrib == null) ? 0 : attrib.toString().hashCode());
			result = prime * result + ((attribValue == null) ? 0 : attribValue.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof Modification) {
				Modification mod = (Modification) obj;

				return (mod.getOuterType().equals(getOuterType()) && mod.attrib.equals(attrib) && mod.attribValue.equals(attribValue));
			}
			else {
				return false;
			}
		}


		/**
		 * Return the enclosing {@link ModificationQueue} object
		 *
		 * @return The enclosing ModificationQueue object
		 */
		private ModificationQueue getOuterType() {
			return ModificationQueue.this;
		}
	}
}
