package com.mongodb.tasktracker.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import org.bson.types.ObjectId


open class Task(_name: String = "Task", project: String = "My Project") : RealmObject() {
    @PrimaryKey @Required var _id: ObjectId = ObjectId()
    var _partition: String = project
    @Required var name: String = _name

    @Required
    private var status: String = TaskStatus.Open.name
    var statusEnum: TaskStatus
        get() {
            return try {
                TaskStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                TaskStatus.Open
            }
        }
        set(value) { status = value.name }
}